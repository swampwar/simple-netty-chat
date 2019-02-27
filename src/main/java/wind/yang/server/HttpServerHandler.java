package wind.yang.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.util.ArrayList;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@ChannelHandler.Sharable
public class HttpServerHandler extends ChannelInboundHandlerAdapter {
    private static final String HTML_FILE = "/main.html";
    private static final String WEBSOCKET_PATH = "/websocket";
    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final ObjectMapper mapper = new ObjectMapper();

    Message message = new Message(new ArrayList<String>(), "", "");
    private WebSocketServerHandshaker handshaker;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            logHttpRequest(request);

            if ("/".equals(request.uri())) {

                processHttpMainReq(ctx, request); // main.html 전송
                return;

            } else if ("/favicon".equals(request.uri())) {

                processHttpNotSupportedReq(ctx, request); // 404 Not-Found
                return;

            } else if (isWebsocketRequest(request.headers())) {

                //Do the Handshake to upgrade connection from HTTP to WebSocket protocol
                handleHandshake(ctx, request);
                System.out.println("Handshake is done");

                //Add User
                addUser(ctx);

                return;
            }

        } else if (msg instanceof WebSocketFrame) {

            processWebSocketRequst(ctx, (WebSocketFrame) msg);

        }

    }

    /**
     * Websocket 연결요청인지 확인
     */
    private boolean isWebsocketRequest(HttpHeaders headers){
        return (headers.get(HttpHeaderNames.CONNECTION).toLowerCase().indexOf("upgrade") > -1 &&
                headers.get(HttpHeaderNames.UPGRADE).toLowerCase().indexOf("websocket") > -1);
    }

    /**
     * 연결완료된 신규 Websocket을 추가(users, channels)
     */
    private void addUser(ChannelHandlerContext ctx){
        String userId = getUserId(ctx.channel());

        // UserList 추가
        message.addUser(userId);

        // Chatroom Channels 추가
        channels.add(ctx.channel());
    }

    /**
     * 연결종료된 Websocket을 제외(users, channels)
     */
    private void removeUser(ChannelHandlerContext ctx){
        String userId = getUserId(ctx.channel());

        // UserList 제외
        message.removeUser(userId);

        // Chatroom Channels 제외
        channels.remove(ctx.channel());
    }

    /**
     * HttpRequest 로깅
     */
    private void logHttpRequest(HttpRequest request) {
        HttpHeaders headers = request.headers();
        System.out.println("HTTP header Connection : " + headers.get(HttpHeaderNames.CONNECTION));
        System.out.println("HTTP header UPGRADE : " + headers.get(HttpHeaderNames.UPGRADE));
    }

    /**
     * 지원되지 않는 Http Request에 대해 처리
     */
    private void processHttpNotSupportedReq(ChannelHandlerContext ctx, HttpRequest request) {
        FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);

        ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
        res.content().writeBytes(buf);
        buf.release();
        HttpUtil.setContentLength(res, res.content().readableBytes());

        ctx.channel().writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
        return;
    }

    /**
     * main.html을 포함하는 response를 생성하여 전송
     */
    private void processHttpMainReq(ChannelHandlerContext ctx, HttpRequest request) throws IOException {
        String htmlFile = readHtmlFile(HTML_FILE);
        FullHttpResponse response = createResponse(request, htmlFile);

        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (!keepAlive) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response);
        }

        return;
    }

    /**
     * WebSocket 요청에 대한 Handshake 수행
     */
    protected void handleHandshake(ChannelHandlerContext ctx, HttpRequest req) {
        String websocketLocation = getWebSocketLocation(req);

        System.out.println("------- Handshaking Start -------");
        System.out.printf("Channel  : [%s] \n", ctx.channel());
        System.out.printf("Websocket Location  : [%s] \n", websocketLocation);

        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                websocketLocation, null, true, 5 * 1024 * 1024);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }

        System.out.println("------- Handshaking Done -------");
    }

    /**
     * websocket URL 생성
     */
    private static String getWebSocketLocation(HttpRequest req) {
        String rsltUrl = "ws://" + req.headers().get(HttpHeaderNames.HOST) + WEBSOCKET_PATH;
        System.out.println("rsltUrl : " + rsltUrl);
        return rsltUrl;
    }

    /**
     * contents를 포함하는 response를 생성
     */
    private FullHttpResponse createResponse(HttpRequest request, String contents) {
        ByteBuf contentBuf = Unpooled.buffer(1000);
        contentBuf.writeBytes(contents.getBytes(CharsetUtil.UTF_8));
        FullHttpResponse response =
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, contentBuf);

        response.headers().set("Content-Type", "text/html; charset=UTF-8");
        response.headers().setInt("Content-Length", response.content().readableBytes());
        response.content().writeBytes(contents.getBytes(CharsetUtil.UTF_8));
        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set("Connection", "keep-alive");
        }

        return response;
    }

    /**
     * File을 읽어 String으로 반환
     */
    private String readHtmlFile(String fileName) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(getClass().getResourceAsStream(fileName));
        String rslt = IOUtils.toString(bis, Charsets.toCharset(CharsetUtil.UTF_8));
        bis.close();
        return rslt;
    }

    /**
     * channel에서 UserId를 생성
     */
    private String getUserId(Channel channel){
        String port = "0000";
        SocketAddress address = channel.remoteAddress();

        if(address instanceof InetSocketAddress){
            InetSocketAddress inetSocketAddr = (InetSocketAddress) address;
            port = Integer.toString(inetSocketAddr.getPort());
        }

        return "User" + port;
    }

    /**
     * Websocket 요청에 대해 처리
     */
    private void processWebSocketRequst(ChannelHandlerContext ctx, WebSocketFrame msg) throws JsonProcessingException {
        System.out.println("This is a WebSocket frame");
        System.out.println("Client Channel : " + ctx.channel());

        if (msg instanceof BinaryWebSocketFrame) {
            System.out.println("BinaryWebSocketFrame Received : ");
            System.out.println(((BinaryWebSocketFrame) msg).content());

        } else if (msg instanceof TextWebSocketFrame) {
            String rText = ((TextWebSocketFrame) msg).text();
            message.setText(rText);
            message.setUser(getUserId(ctx.channel()));
            String sMsg = mapper.writeValueAsString(message);

            System.out.println("TextWebSocketFrame Received : " + rText);
            System.out.println("TextWebSocketFrame Send : " + sMsg);

            channels.writeAndFlush(new TextWebSocketFrame(sMsg));
        } else if (msg instanceof PingWebSocketFrame) {
            System.out.println("PingWebSocketFrame Received : ");
            System.out.println(((PingWebSocketFrame) msg).content());
        } else if (msg instanceof PongWebSocketFrame) {
            System.out.println("PongWebSocketFrame Received : ");
            System.out.println(((PongWebSocketFrame) msg).content());
        } else if (msg instanceof CloseWebSocketFrame) {
            System.out.println("CloseWebSocketFrame Received : ");
            System.out.println("ReasonText :" + ((CloseWebSocketFrame) msg).reasonText());
            System.out.println("StatusCode : " + ((CloseWebSocketFrame) msg).statusCode());

            removeUser(ctx);

            message.setText("내가 간다.");
            message.setUser(getUserId(ctx.channel()));

            String sMsg = mapper.writeValueAsString(message);

            channels.writeAndFlush(new TextWebSocketFrame(sMsg));
            ctx.channel().writeAndFlush(new TextWebSocketFrame(sMsg)).addListener(ChannelFutureListener.CLOSE);

        } else {
            System.out.println("Unsupported WebSocketFrame");
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
