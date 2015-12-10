package com.jrjackson;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.jruby.*;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.runtime.ThreadContext;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyAnySerializer extends StdSerializer<IRubyObject> {

    /**
     * Singleton instance to use.
     */
    public static final RubyAnySerializer instance = new RubyAnySerializer();
    private static final HashMap<Class, Class> class_maps = new HashMap<Class, Class>();

    static {
        // not need now - clean up required
        class_maps.put(RubyBoolean.class, Boolean.class);
        class_maps.put(RubyFloat.class, Double.class);
        class_maps.put(RubyFixnum.class, Long.class);
        class_maps.put(RubyBignum.class, BigInteger.class);
        class_maps.put(RubyBigDecimal.class, BigDecimal.class);
    }

    public RubyAnySerializer() {
        super(IRubyObject.class);
    }

    private Class<?> rubyJavaClassLookup(Class target) {
        return class_maps.get(target);
    }

    private void serializeUnknownRubyObject(ThreadContext ctx, IRubyObject rubyObject, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException {
        RubyClass meta = rubyObject.getMetaClass();

        DynamicMethod method = meta.searchMethod("to_time");
        if (!method.isUndefined()) {
            RubyTime dt = (RubyTime) method.call(ctx, rubyObject, meta, "to_time");
            String time = RubyUtils.jodaTimeString(dt.getDateTime());
            jgen.writeString(time);
            return;
        }

        method = meta.searchMethod("to_h");
        if (!method.isUndefined()) {
            RubyObject obj = (RubyObject) method.call(ctx, rubyObject, meta, "to_h");
            provider.findTypedValueSerializer(Map.class, true, null).serialize(obj, jgen, provider);
            return;
        }

        method = meta.searchMethod("to_hash");
        if (!method.isUndefined()) {
            RubyObject obj = (RubyObject) method.call(ctx, rubyObject, meta, "to_hash");
            provider.findTypedValueSerializer(Map.class, true, null).serialize(obj, jgen, provider);
            return;
        }

        method = meta.searchMethod("to_a");
        if (!method.isUndefined()) {
            RubyObject obj = (RubyObject) method.call(ctx, rubyObject, meta, "to_a");
            provider.findTypedValueSerializer(List.class, true, null).serialize(obj, jgen, provider);
            return;
        }

        method = meta.searchMethod("to_json");
        if (!method.isUndefined()) {
            RubyObject obj = (RubyObject) method.call(ctx, rubyObject, meta, "to_json");
            if (obj instanceof RubyString) {
                jgen.writeRawValue(obj.toString());
            } else {
                provider.defaultSerializeValue(obj, jgen);
            }
            return;
        }
        throw new JsonGenerationException("Cannot find Serializer for class: " + rubyObject.getClass().getName());
    }

    @Override
    public void serialize(IRubyObject value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException {
        ThreadContext ctx = value.getRuntime().getCurrentContext();
        if (value.isNil()) {

            jgen.writeNull(); // for RubyNil and NullObjects

        } else if (value instanceof RubyString) {

            jgen.writeString(value.toString());

        } else if (value instanceof RubySymbol) {

            jgen.writeString(value.toString());

        } else if (value instanceof RubyBoolean) {

            jgen.writeBoolean(value.isTrue());

        } else if (value instanceof RubyFloat) {

            jgen.writeNumber(RubyNumeric.num2dbl(value));

        } else if (value instanceof RubyFixnum) {

            jgen.writeNumber(RubyNumeric.num2long(value));

        } else if (value instanceof RubyBignum) {

            jgen.writeNumber(((RubyBignum) value).getBigIntegerValue());

        } else if (value instanceof RubyBigDecimal) {

            jgen.writeNumber(((RubyBigDecimal) value).getBigDecimalValue());

        } else if (value instanceof RubyHash) {

            provider.findTypedValueSerializer(value.getJavaClass(), true, null).serialize(value, jgen, provider);

        } else if (value instanceof RubyArray) {

            provider.findTypedValueSerializer(value.getJavaClass(), true, null).serialize(value, jgen, provider);

        } else if (value instanceof RubyStruct) {

            IRubyObject obj = value.callMethod(ctx, "to_a");
            provider.findTypedValueSerializer(obj.getJavaClass(), true, null).serialize(obj, jgen, provider);

        } else {

            Class<?> cls = rubyJavaClassLookup(value.getClass());
            if (cls != null) {
                Object val = value.toJava(cls);
                if (val != null) {
                    provider.defaultSerializeValue(val, jgen);
                } else {
                    serializeUnknownRubyObject(ctx, value, jgen, provider);
                }
            } else {
                serializeUnknownRubyObject(ctx, value, jgen, provider);
            }

        }
    }

    /**
     * Default implementation will write type prefix, call regular serialization method (since assumption is that value itself does not need JSON Array or Object start/end markers), and then write type suffix. This should work for most cases; some sub-classes may want to change this behavior.
     *
     * @param value
     * @param jgen
     * @param provider
     * @param typeSer
     * @throws java.io.IOException
     * @throws com.fasterxml.jackson.core.JsonGenerationException
     */
    @Override
    public void serializeWithType(IRubyObject value, JsonGenerator jgen, SerializerProvider provider, TypeSerializer typeSer)
            throws IOException, JsonGenerationException {
        typeSer.writeTypePrefixForScalar(value, jgen);
        serialize(value, jgen, provider);
        typeSer.writeTypeSuffixForScalar(value, jgen);
    }
}
