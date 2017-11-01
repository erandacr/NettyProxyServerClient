/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package wso2.support.work.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.socket.oio.OioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyConnectException;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.NetUtil;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

/**
 * A simple HTTP client that prints out the content of the HTTP response to
 * {@link System#out} to test
 */
public final class HttpClient {

//    static final String URL = System.getProperty("url", "http://localhost:8080/stockquote");
    // temp
    static final String URL = System.getProperty("url", "http://10.100.5.40:9090");

    private static void sendReceive(URI uri, HttpRequest request) throws Exception {

        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            if ("http".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("https".equalsIgnoreCase(scheme)) {
                port = 443;
            }
        }

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            System.err.println("Only HTTP(S) is supported.");
            return;
        }

        // Configure SSL context if necessary.
        final boolean ssl = "https".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        final HttpClientHandler clientHandler = new HttpClientHandler();
        //final HttpProxyHandler proxyHandler = new HttpProxyHandler(new InetSocketAddress(NetUtil.LOCALHOST, 8080));

        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .handler(new HttpClientInitializer(sslCtx));
//            b.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
//                @Override
//                public void initChannel(SocketChannel ch) throws Exception {
//                    ch.pipeline().addLast(
//                            new HttpProxyHandler(new InetSocketAddress(NetUtil.LOCALHOST, 3128), "admin", "admin"),
//                            new HttpClientCodec(),
//                            new HttpContentDecompressor(),
//                            new HttpObjectAggregator(1048576),
//                            clientHandler
//                    );
//                }
//            });



            // Make the connection attempt.
            Channel ch = b.connect(host, port).sync().channel();

            // Prepare the HTTP request.
            //            HttpRequest request = new DefaultFullHttpRequest(
            //                    HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath());
            //            request.headers().set(HttpHeaderNames.HOST, host);
            //            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            //            request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
            //
            //            // Set some example cookies.
            //            request.headers().set(
            //                    HttpHeaderNames.COOKIE,
            //                    ClientCookieEncoder.STRICT.encode(
            //                            new DefaultCookie("my-cookie", "foo"),
            //                            new DefaultCookie("another-cookie", "bar")));

//            FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.DELETE,
//                    uri.getRawPath());
//            request.headers().set(HttpHeaderNames.HOST, host);
//            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
//            request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
//
//            request.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
//            ByteBuf bbuf = Unpooled
//                    .copiedBuffer("{\"symbol\":\"BAR2\",\"name\": \"Bar Inc.\",\"last\":149.62,\"low\":150.78,\"high\":149.18}",
//                            StandardCharsets.UTF_8);
//            request.headers().set(HttpHeaderNames.CONTENT_LENGTH, bbuf.readableBytes());
//            request.content().clear().writeBytes(bbuf);
            // Send the HTTP request.
            ch.writeAndFlush(request);
//            future.awaitUninterruptibly();
//            future.await();
//            final CountDownLatch notified = new CountDownLatch(1);
//            future.addListener(new ChannelFutureListener() {
//                @Override
//                public void operationComplete(ChannelFuture future) throws Exception {
//                    notified.countDown();
//                }
//            });



            // Wait for the server to close the connection.
            ch.closeFuture().sync();
//            System.out.println(clientHandler.getMsg().toString());
        } finally {
            // Shut down executor threads to exit.
            group.shutdownGracefully();
        }
    }

    private static HttpRequest createGET(URI uri) {
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath());
        request.headers().set("host", uri.getHost());
        request.headers().set("connection", "close");
        request.headers().set("accept-encoding", "gzip");
        return request;

    }

    private static HttpRequest createPOST(URI uri) {
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,
                uri.getRawPath());
        request.headers().set(HttpHeaderNames.HOST, uri.getHost());
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);

        request.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        ByteBuf bbuf = Unpooled
                .copiedBuffer("{\"symbol\":\"BAR2\",\"name\": \"Bar Inc.\",\"last\":149.62,\"low\":150.78,\"high\":149.18}",
                        StandardCharsets.UTF_8);
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, bbuf.readableBytes());
        request.content().clear().writeBytes(bbuf);

        return request;
    }

    private static HttpRequest createDELETE(URI uri) {
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.DELETE,
                uri.getRawPath());
        request.headers().set("host", uri.getHost());
        request.headers().set("connection", "close");
        request.headers().set("accept-encoding", "gzip");

        request.headers().add("content-type", "application/json");
        ByteBuf bbuf = Unpooled
                .copiedBuffer("{\"symbol\":\"BAR2\",\"name\": \"Bar Inc.\",\"last\":149.62,\"low\":150.78,\"high\":149.18}",
                        StandardCharsets.UTF_8);
        request.headers().set("content-length", bbuf.readableBytes());
        request.content().clear().writeBytes(bbuf);

        return request;
    }

    public static void main(String[] args) throws Exception {
        URI uri;


        //Temp
        System.out.println("\nSending GET\n");
        uri = new URI(URL);
        sendReceive(uri, createGET(uri));
        System.out.println("\n====== get 1 done========");



//        //POST
        System.out.println("\nSending POST\n");
        uri = new URI(URL);
        sendReceive(uri, createPOST(uri));
        System.out.println("\n====post done====");

        //GET
        System.out.println("\nSending GET\n");
        uri = new URI(URL+"/all");
        sendReceive(uri, createGET(uri));
        System.out.println("\n====== get 1 done========");

//        //DELETE
//        System.out.println("\nSending DELETE\n");
//        uri = new URI(URL);
//        sendReceive(uri, createDELETE(uri));
//        System.out.println("\n========= delete done =======");
//
//        //GET
//        System.out.println("\nSending second GET\n");
//        uri = new URI(URL+"/all");
//        sendReceive(uri, createGET(uri));
//        System.out.println("\n========get 2 done");
//
//        System.out.println("finished=========================================");
    }

}