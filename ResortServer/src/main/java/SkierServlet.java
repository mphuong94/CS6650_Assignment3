import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import util.ChannelFactory;
import util.LiftInfo;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "SkierServlet", value = "/SkierServlet")
public class SkierServlet extends HttpServlet {
    public static ConnectionFactory factory;
    public static ObjectPool<Channel> pool;
    public GsonBuilder builder;
    public Gson gson;
    private static String EXCHANGE_NAME = "postRequest";
    final static Logger logger = Logger.getLogger(SkierServlet.class.getName());
    private static final String RABBIT_HOST = "34.219.103.201";

    @Override
    public void init() {
        factory = new ConnectionFactory();
        factory.setHost(RABBIT_HOST);
        try {
            Connection newConn = factory.newConnection();
            GenericObjectPoolConfig<Channel> config = new GenericObjectPoolConfig<>();
            config.setMaxTotal(200);
            config.setMinIdle(100);
            config.setMaxIdle(200);
            pool = new GenericObjectPool<>(new ChannelFactory(newConn), config);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        builder = new GsonBuilder();
        builder.setPrettyPrinting();
        gson = builder.create();
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        String urlPath = request.getPathInfo();

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("missing parameters");
            return;
        }

        String[] urlParts = urlPath.split("/");
        // and now validate url path and return the response status code
        // (and maybe also some value if input is valid)

        if (!validateGet(urlParts)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("It works!");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        String urlPath = request.getPathInfo();
        logger.log(Level.INFO,urlPath);
        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("missing parameters");
            return;
        }
        if (!validatePost(request)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            logger.log(Level.INFO, request.getParameter("skier_id"));
            int skierId = Integer.parseInt(request.getParameter("skier_id"));
            int liftId = Integer.parseInt(request.getParameter("lift_id"));
            int minute = Integer.parseInt(request.getParameter("minute"));
            int waitTime = Integer.parseInt(request.getParameter("wait"));
            LiftInfo newInfo = new LiftInfo(skierId,liftId,minute,waitTime);
            try {
                Channel channel = pool.borrowObject();
                String jsonString = gson.toJson(newInfo);
                channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
                channel.basicPublish(EXCHANGE_NAME, "", null, jsonString.getBytes("UTF-8"));
                pool.returnObject(channel);
                response.setStatus(HttpServletResponse.SC_CREATED);
                response.getWriter().write("It works post!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean validateGet(String[] urlPath) {
        // urlPath  = "/1/seasons/2019/day/1/skier/123"
        // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
        if (urlPath.length == 8){
            return true;
        }
        return false;
    }

    private boolean validatePost(HttpServletRequest request) {
//        logger.log(Level.INFO, request.getParameter("skiers_ids"));
        Map paramsSupplied = request.getParameterMap();
        if (paramsSupplied.containsKey("skier_id")){
            if (Integer.parseInt(request.getParameter("skier_id")) > 100000){
                return false;
            }
        }
        else {
            return false;
        }
        if (paramsSupplied.containsKey("lift_id")){
            if (Integer.parseInt(request.getParameter("lift_id")) > 60){
                return false;
            }
        }
        else {
            return false;
        }
        if (paramsSupplied.containsKey("minute")){
            if (Integer.parseInt(request.getParameter("minute")) > 420){
                return false;
            }
        }
        else {
            return false;
        }
        if (paramsSupplied.containsKey("wait")){
            if (Integer.parseInt(request.getParameter("wait")) > 10){
                return false;
            }
        }
        else {
            return false;
        }
        return true;
    }
}
