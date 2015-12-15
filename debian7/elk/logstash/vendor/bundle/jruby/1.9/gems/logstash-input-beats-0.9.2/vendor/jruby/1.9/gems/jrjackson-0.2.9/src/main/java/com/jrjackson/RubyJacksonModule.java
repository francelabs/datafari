package com.jrjackson;


import org.jruby.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyJacksonModule extends SimpleModule {

    private static final ObjectMapper static_mapper = new ObjectMapper().registerModule(
            new RubyJacksonModule().addSerializer(IRubyObject.class, RubyAnySerializer.instance)
    );

    static {
        static_mapper.registerModule(new AfterburnerModule());
        static_mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private RubyJacksonModule() {
        super("JrJacksonStrModule", VersionUtil.versionFor(RubyJacksonModule.class));
    }

    public static ObjectMapper mappedAs(String key, Ruby ruby) {
        if ("raw".equals(key)) {
            return static_mapper;
        }

        ObjectMapper mapper = new ObjectMapper().registerModule(
                new AfterburnerModule()
        );

        if ("sym".equals(key)) {
            mapper.registerModule(
                    asSym(ruby)
            );
        } else {
            mapper.registerModule(
                    asStr(ruby)
            );
        }
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    public static SimpleModule asSym(Ruby ruby) {
        return new RubyJacksonModule().addSerializer(
                IRubyObject.class, RubyAnySerializer.instance
        ).addDeserializer(
                Object.class, new RubyObjectDeserializer().withRuby(ruby).setSymbolStrategy()
        );
    }

    public static SimpleModule asStr(Ruby ruby) {
        return new RubyJacksonModule().addSerializer(
                IRubyObject.class, RubyAnySerializer.instance
        ).addDeserializer(
                Object.class, new RubyObjectDeserializer().withRuby(ruby).setStringStrategy()
        );
    }
}
