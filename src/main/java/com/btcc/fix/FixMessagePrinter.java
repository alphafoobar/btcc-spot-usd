package com.btcc.fix;

import quickfix.*;
import quickfix.field.MsgType;

import java.util.Iterator;

public class FixMessagePrinter {

    public static void print(DataDictionary dd, Message message) {
        try {
            String msgType = message.getHeader().getString(MsgType.FIELD);

            String msgName = dd.getValueName(MsgType.FIELD, msgType);

            System.out.println("===========================================================");

            for (int i = 0; i < (57 - msgName.length()) / 2; i++) {
                System.out.print(' ');
            }
            System.out.println(msgName);
            System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
            printFieldMap("    ", dd, msgType, message.getHeader());
            printFieldMap("    ", dd, msgType, message);
            printFieldMap("    ", dd, msgType, message.getTrailer());

            System.out.println();
        } catch (FieldNotFound e) {

        }
    }

    private static void printFieldMap(String prefix, DataDictionary dd, String msgType, FieldMap fieldMap)
            throws FieldNotFound {

        Iterator fieldIterator = fieldMap.iterator();
        while (fieldIterator.hasNext()) {
            Field field = (Field) fieldIterator.next();
            if (!isGroupCountField(dd, field)) {
                String value = fieldMap.getString(field.getTag());
                if (dd.hasFieldValue(field.getTag())) {
                    value = dd.getValueName(field.getTag(), fieldMap.getString(field.getTag())) + " (" + value + ")";
                }
                System.out.println(prefix + String.format("%-18s%s", dd.getFieldName(field.getTag()) + ": ", value));
            }
        }

        Iterator groupsKeys = fieldMap.groupKeyIterator();
        while (groupsKeys.hasNext()) {
            int groupCountTag = (int) groupsKeys.next();
            System.out.println(prefix + dd.getFieldName(groupCountTag) + ": count = "
                    + fieldMap.getInt(groupCountTag));
            quickfix.Group g = new quickfix.Group(groupCountTag, 0);
            int i = 1;
            while (fieldMap.hasGroup(i, groupCountTag)) {
                if (i > 1) {
                    System.out.println(prefix + "    ----");
                }
                fieldMap.getGroup(i, g);
                printFieldMap(prefix + "    ", dd, msgType, g);
                i++;
            }
        }
    }

    private static boolean isGroupCountField(DataDictionary dd, Field field) {
        return dd.getFieldTypeEnum(field.getTag()) == FieldType.NumInGroup;
    }
}
