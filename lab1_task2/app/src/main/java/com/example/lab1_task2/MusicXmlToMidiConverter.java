package com.example.lab1_task2;

import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MusicXmlToMidiConverter {

    public static void convert(InputStream xmlInput, File outputFile) throws Exception {
        List<ScoreNoteData> notes = parseMusicXml(xmlInput);
        byte[] midiData = generateMidi(notes);
        
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(midiData);
        }
    }

    private static List<ScoreNoteData> parseMusicXml(InputStream input) throws Exception {
        List<ScoreNoteData> notes = new ArrayList<>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(input, null);

        int eventType = parser.getEventType();
        String currentStep = "C";
        int currentOctave = 4;
        int currentDuration = 1;
        int divisions = 1;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String name = parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if ("divisions".equals(name)) {
                    String text = parser.nextText();
                    if (text != null && !text.isEmpty()) {
                        divisions = Integer.parseInt(text);
                    }
                } else if ("step".equals(name)) {
                    currentStep = parser.nextText();
                } else if ("octave".equals(name)) {
                    String text = parser.nextText();
                    if (text != null && !text.isEmpty()) {
                        currentOctave = Integer.parseInt(text);
                    }
                } else if ("duration".equals(name)) {
                    String text = parser.nextText();
                    if (text != null && !text.isEmpty()) {
                        currentDuration = Integer.parseInt(text);
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if ("note".equals(name)) {
                    notes.add(new ScoreNoteData(currentStep, currentOctave, currentDuration, divisions));
                }
            }
            eventType = parser.next();
        }
        return notes;
    }

    private static byte[] generateMidi(List<ScoreNoteData> notes) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Header Chunk: MThd, length 6, format 0, 1 track, 128 ticks per quarter
        baos.write("MThd".getBytes());
        baos.write(new byte[]{0, 0, 0, 6, 0, 0, 0, 1, 0, (byte) 0x80}); 
        
        // Track Chunk
        ByteArrayOutputStream trackData = new ByteArrayOutputStream();
        int ppq = 128;
        
        for (ScoreNoteData note : notes) {
            int midiPitch = note.getMidiPitch();
            int durationTicks = (note.duration * ppq) / (note.divisions > 0 ? note.divisions : 1);
            
            // Note On
            writeVarInt(trackData, 0); 
            trackData.write(0x90); 
            trackData.write(midiPitch);
            trackData.write(64); // Velocity
            
            // Note Off
            writeVarInt(trackData, durationTicks); 
            trackData.write(0x80); 
            trackData.write(midiPitch);
            trackData.write(0); // Velocity
        }
        
        // End of track: FF 2F 00
        writeVarInt(trackData, 0);
        trackData.write(new byte[]{(byte) 0xFF, 0x2F, 0x00});
        
        byte[] trackBytes = trackData.toByteArray();
        baos.write("MTrk".getBytes());
        baos.write(intToByteArray(trackBytes.length));
        baos.write(trackBytes);
        
        return baos.toByteArray();
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        int v = value;
        byte[] buffer = new byte[4];
        int i = 0;
        buffer[i++] = (byte) (v & 0x7F);
        while ((v >>= 7) > 0) {
            buffer[i++] = (byte) ((v & 0x7F) | 0x80);
        }
        while (i > 0) {
            out.write(buffer[--i]);
        }
    }

    private static byte[] intToByteArray(int value) {
        return new byte[] {
            (byte)(value >>> 24),
            (byte)(value >>> 16),
            (byte)(value >>> 8),
            (byte)value
        };
    }
}
