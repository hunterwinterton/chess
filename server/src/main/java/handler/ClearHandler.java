package handler;

import com.google.gson.Gson;
import service.ClearService;
import spark.Request;
import spark.Response;

import java.util.Map;

public class ClearHandler {
    private final ClearService service;
    private final Gson gson = new Gson();

    public ClearHandler(ClearService service) {
        this.service = service;
    }

    public Object clear(Request req, Response res) {
        try {
            service.clear();
            res.status(200);
            return "{}";
        } catch (Exception e) {
            return handleException(e, res);
        }
    }

    private Object handleException(Exception e, Response res) {
        res.status(500);
        return gson.toJson(Map.of("message", e.getMessage()));
    }
}