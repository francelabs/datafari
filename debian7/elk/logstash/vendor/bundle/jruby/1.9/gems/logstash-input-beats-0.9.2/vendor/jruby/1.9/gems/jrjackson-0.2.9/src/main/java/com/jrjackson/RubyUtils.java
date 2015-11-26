package com.jrjackson;

import java.util.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import org.jruby.*;
import org.jruby.javasupport.JavaUtil;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.util.SafeDoubleParser;

public class RubyUtils {

    private final static DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss Z").withLocale(Locale.ENGLISH);
    private final static DateTimeFormatter UTC_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withLocale(Locale.ENGLISH);

    public static RubyObject rubyObject(Ruby ruby, Object node) {
        return (RubyObject) JavaUtil.convertJavaToRuby(ruby, node);
    }

    public static RubyString rubyString(Ruby ruby, String node) {
        return RubyString.newUnicodeString(ruby, node);
    }

    public static RubyString rubyString(Ruby ruby, byte[] node) {
        return RubyString.newString(ruby, node);
    }

    public static RubyString rubyString(Ruby ruby, CharSequence node) {
        return RubyString.newUnicodeString(ruby, node);
    }

    public static RubySymbol rubySymbol(Ruby ruby, String node) {
        return RubySymbol.newSymbol(ruby, node);
    }

    public static RubyArray rubyArray(Ruby ruby, Object[] arg) {
        return (RubyArray) JavaUtil.convertJavaToRuby(ruby, arg);
    }

    public static RubyArray rubyArray(Ruby ruby, List arg) {
        return (RubyArray) JavaUtil.convertJavaToRuby(ruby, arg);
    }

    public static RubyHash rubyHash(Ruby ruby, Map arg) {
        return (RubyHash) JavaUtil.convertJavaToRuby(ruby, arg);
    }

    public static RubyFixnum rubyFixnum(Ruby ruby, int arg) {
        return ruby.newFixnum(arg);
    }

    public static RubyFixnum rubyFixnum(Ruby ruby, long arg) {
        return ruby.newFixnum(arg);
    }

    public static RubyBignum rubyBignum(Ruby ruby, BigInteger arg) {
        return RubyBignum.newBignum(ruby, arg);
    }

    public static RubyFloat rubyFloat(Ruby ruby, double arg) {
        return ruby.newFloat(arg);
    }

    public static RubyFloat rubyFloat(Ruby ruby, String arg) {
        double d = SafeDoubleParser.parseDouble(arg);
        return ruby.newFloat(d);
    }

    public static RubyBigDecimal rubyBigDecimal(Ruby ruby, BigDecimal arg) {
        return new RubyBigDecimal(ruby, arg);
    }

    public static RubyBoolean rubyBoolean(Ruby ruby, Boolean arg) {
        return ruby.newBoolean(arg);
    }

    public static String jodaTimeString(DateTime dt) {
        // copied from the RubyTime to_s method
        // to prevent the double handling of a String -> RubyString -> String
        DateTimeFormatter simpleDateFormat;
        if (dt.getZone() == DateTimeZone.UTC) {
            simpleDateFormat = UTC_FORMATTER;
        } else {
            simpleDateFormat = FORMATTER;
        }

        return simpleDateFormat.print(dt);

        // JrJackson: no access to private boolean isTzRelative

//        String result = simpleDateFormat.print(dt);
//
//        if (isTzRelative) {
//            // display format needs to invert the UTC offset if this object was
//            // created with a specific offset in the 7-arg form of #new
//            DateTimeZone dtz = dt.getZone();
//            int offset = dtz.toTimeZone().getOffset(dt.getMillis());
//            DateTimeZone invertedDTZ = DateTimeZone.forOffsetMillis(offset);
//            DateTime invertedDT = dt.withZone(invertedDTZ);
//            result = simpleDateFormat.print(invertedDT);
//        }
//
//        return result;
    }
}
