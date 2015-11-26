package com.jrjackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonStreamContext;
import java.io.IOException;
import java.util.HashMap;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.runtime.builtin.IRubyObject;


/**
 *
 * @author Guy Boertje
 */
public class JrParse {
    private final RubyHandler _handler;
    protected final HashMap<JsonStreamContext, IRubyObject> _objectMap = new HashMap<JsonStreamContext, IRubyObject>();
    protected JsonStreamContext _deepestContext;
    
    public JrParse(RubyHandler handler) {
        _handler = handler;
        
    }
    
    public void deserialize(JsonParser jp) throws JsonProcessingException, IOException {
        try {
            
            while (jp.nextValue() != null) {
                handleCurrentToken(jp);
            }
            
        } catch (JsonProcessingException e) {
            _handler.raiseError(e.getLocalizedMessage());
        } catch (IOException e) {
            _handler.raiseError(e.getLocalizedMessage());
        }
    }
    
    private void callAddValue(JsonStreamContext x) {
        JsonStreamContext px = x.getParent();
        IRubyObject dtarget = _objectMap.get(_deepestContext);
        
        if (px == null) {
            _handler.addValue(dtarget);
            return;
        }
        
        IRubyObject value = _objectMap.get(x);

        if (x.inArray()) {
            _handler.arrayAppend(
                    (RubyArray)value, dtarget);
        } else if (x.inObject()) {
            _handler.hashSet(
                    (RubyHash)value, callHashKey(x), dtarget);

        } else {
            _handler.addValue(value);
        }
    }
    
    private void callAddValue(JsonStreamContext x, IRubyObject val) {
        
        if (x.inArray()) {
            RubyArray a = (RubyArray)_objectMap.get(x);
            _handler.arrayAppend(a, val);
        } else if (x.inObject()) {
            RubyHash h = (RubyHash)_objectMap.get(x);
            _handler.hashSet(h, callHashKey(x), val);
            
        } else {
            _handler.addValue(val);
        }
    }
    
    private IRubyObject callHashKey(JsonStreamContext x) {
        String k = x.getCurrentName();
        if (k == null) {
            return _handler.treatNull();
        }
        return _handler.hashKey(k);
    }
    
    private void handleCurrentToken(JsonParser jp)
            throws IOException, JsonProcessingException {
        
        JsonStreamContext cx = jp.getParsingContext();
        
        switch (jp.getCurrentToken()) {
            case START_OBJECT:
                _deepestContext = cx;
                _objectMap.put(cx, _handler.hashStart());
                break;
                
            case START_ARRAY:
                _deepestContext = cx;
                _objectMap.put(cx, _handler.arrayStart());
                
            case FIELD_NAME:
                break;
                
            case VALUE_EMBEDDED_OBJECT:
                System.out.println("-------- VALUE_EMBEDDED_OBJECT ????????? --------");
                System.out.println(jp.getEmbeddedObject());
                break;
                
            case VALUE_STRING:
                callAddValue(cx, 
                        _handler.treatString(jp));
                break;
            
            case VALUE_NUMBER_INT:
                callAddValue(cx,
                        _handler.treatInt(jp));
                break;
                
            case VALUE_NUMBER_FLOAT:
                callAddValue(cx,
                        _handler.treatFloat(jp));
                break;
                
            case VALUE_TRUE:
                callAddValue(cx, _handler.trueValue());
                break;

            case VALUE_FALSE:
                callAddValue(cx, _handler.falseValue());
                break;

            case VALUE_NULL: // should not get this but...
                callAddValue(cx, _handler.treatNull());
                break;

            case END_ARRAY:
                _handler.arrayEnd();
                callAddValue(cx);
                _deepestContext = cx;
                break;

            case END_OBJECT:
                _handler.hashEnd();
                callAddValue(cx);
                _deepestContext = cx;
                break;
        }
    }
}
