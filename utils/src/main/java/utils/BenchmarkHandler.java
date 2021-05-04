package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class BenchmarkHandler {

    public static BenchmarkHandler instance;
    private static Logger log = LoggerFactory.getLogger(BenchmarkHandler.class);


    Map<Integer, Long> timerMap;
    Map<Integer, Long> networkMap;

    public static BenchmarkHandler getInstance(){
        if(BenchmarkHandler.instance == null){
            BenchmarkHandler.instance = new BenchmarkHandler();
        }
        return BenchmarkHandler.instance;
    }

    BenchmarkHandler(){
        timerMap = new HashMap<>();
        networkMap = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    void printTime(long time, int id){
        long seconds = time / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        JSONObject timeObject = new JSONObject();
        JSONObject resolutions = new JSONObject();
        timeObject.put("timer", String.valueOf(id));

        resolutions.put("h", String.valueOf(hours));
        resolutions.put("min", String.valueOf(minutes));
        resolutions.put("sec", String.valueOf((double)time / 1000));
        resolutions.put("ms", time);
        timeObject.put("time", resolutions);
        log.info("\n\n<b3m4>" + timeObject.toJSONString() + "</b3m4>\n");
    }

    @SuppressWarnings("unchecked")
    void printNetwork(long receivedBytes, int id){
        long kb = receivedBytes / 1024;
        long mb = kb / 1024;
        long gb = mb / 1024;
        JSONObject received = new JSONObject();
        received.put("bytes", String.valueOf(receivedBytes));
        if(kb > 0){
            received.put("KB", String.valueOf(kb));
        } if(mb > 0){
            received.put("mb", String.valueOf(mb));
        } if(gb > 0){
            received.put("gb", String.valueOf(gb));
        }
        JSONObject print = new JSONObject();
        print.put("player", String.valueOf(id));
        JSONObject recv = new JSONObject();
        recv.put("received", received);
        print.put("netdata", recv);
        log.info("\n\n<b3m4>" + print.toJSONString() + "</b3m4>\n");
    }

    public void startTimer(Integer id){
        if(timerMap.containsKey(id)){
            throw new RuntimeException("Timer for id " + id + " is already running");
        }
        long timer = System.currentTimeMillis();
        timerMap.put(id, timer);
    }

    public void startNetwork(Integer id, long start){
        if(networkMap.containsKey(id)){
            throw new RuntimeException("network tracking for id " + id + " is already running");
        }
        networkMap.put(id, start);
    }

    public void endNetwork(Integer id, long traffic){
        if(!networkMap.containsKey(id)){
            throw new RuntimeException("No network traffic measured under id " + id);
        }
        long start = networkMap.get(id);
        long received = traffic - start;
        printNetwork(received, id);
    }

    public void endTimer(Integer id){
        if(!timerMap.containsKey(id)){
            throw new RuntimeException("No timer running for id " + id);
        }
        long timer = timerMap.get(id);
        long now = System.currentTimeMillis();
        long duration = now - timer;
        printTime(duration, id);
        timerMap.remove(id);
    }

}
