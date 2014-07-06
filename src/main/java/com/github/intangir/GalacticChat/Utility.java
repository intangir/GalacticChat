package com.github.intangir.GalacticChat;

import java.util.List;

public class Utility {
    
    // why doesn't java have any easily accessible join functions...
    static public String join(List<String> args, String connector) {
    	String joined = null;
    	
    	for(String part : args) {
    		if(joined == null) {
    			joined = part;
    		} else {
    			joined += connector + part;
    		}
    	}
    	
    	return joined;
    }

}
