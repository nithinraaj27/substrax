package com.substrax.ledger.util;

import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroJsonUtil {

    public static String toJson(Object avroObject){
        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Encoder encoder = EncoderFactory.get().jsonEncoder(
                    ((org.apache.avro.specific.SpecificRecord) avroObject).getSchema(),
                    out
            );

            SpecificDatumWriter<Object> writer = new SpecificDatumWriter<>(((org.apache.avro.specific.SpecificRecord) avroObject).getSchema());

            writer.write(avroObject, encoder);
            encoder.flush();
            return out.toString();

        } catch (IOException e) {

            throw new RuntimeException("Failed to convert Avro to JSON", e);
        }
    }
}
