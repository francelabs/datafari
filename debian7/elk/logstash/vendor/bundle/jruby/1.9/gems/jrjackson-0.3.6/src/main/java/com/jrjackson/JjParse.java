package com.jrjackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonStreamContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


/**
 *
 * @author Guy Boertje
 */
public class JjParse {
    private final JavaHandler _handler;
    protected final HashMap<JsonStreamContext, Object> _objectMap = new HashMap<JsonStreamContext, Object>();
    protected JsonStreamContext _deepestContext;

    public JjParse(JavaHandler handler) {
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
        Object dtarget = _objectMap.get(_deepestContext);

        if (px == null) {
            _handler.addValue(dtarget);
            return;
        }

        Object value = _objectMap.get(x);

        if (x.inArray()) {
            _handler.arrayAppend(
                    (ArrayList<Object>)value, dtarget);
        } else if (x.inObject()) {
            _handler.hashSet(
                    (HashMap<String, Object>)value, callHashKey(x), dtarget);

        } else {
            _handler.addValue(value);
        }
    }

    private void callAddValue(JsonStreamContext x, Object val) {

        if (x.inArray()) {
            ArrayList<Object> a = (ArrayList<Object>)_objectMap.get(x);
            _handler.arrayAppend(a, val);
        } else if (x.inObject()) {
            HashMap<String, Object> h = (HashMap<String, Object>)_objectMap.get(x);
            _handler.hashSet(h, callHashKey(x), val);

        } else {
            _handler.addValue(val);
        }
    }

    private String callHashKey(JsonStreamContext x) {
        String k = x.getCurrentName();
        if (k == null) {
            return null;
        }
        return (String)_handler.hashKey(k);
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
