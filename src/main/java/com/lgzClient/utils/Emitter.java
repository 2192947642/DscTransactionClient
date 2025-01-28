package com.lgzClient.utils;

import java.util.ArrayList;
import java.util.HashMap;

public class Emitter {
    public static  HashMap<Event, ArrayList<Runnable>> map=new HashMap<>();
    public enum Event{
        Success,
    }
    public static void on(Event event,Runnable runnable){
        if(map.containsKey(event)) map.get(event).add(runnable);
        else {
            ArrayList<Runnable> list=new ArrayList<>();
            list.add(runnable);
            map.put(event,list);
        }
    }
    public static void emit(Event event){
        if(map.containsKey(event)){
            for(Runnable runnable:map.get(event)){
                runnable.run();
            }
        }
    }
}
