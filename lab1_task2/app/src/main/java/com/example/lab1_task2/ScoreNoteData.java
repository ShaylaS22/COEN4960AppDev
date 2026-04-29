package com.example.lab1_task2;

public class ScoreNoteData {
    public String step;
    public int octave;
    public int duration;
    public int divisions;

    public ScoreNoteData(String step, int octave, int duration, int divisions) {
        this.step = step;
        this.octave = octave;
        this.duration = duration;
        this.divisions = divisions;
    }

    public int getMidiPitch() {
        int base = 0;
        if (step == null) return 60;
        switch (step.toUpperCase()) {
            case "C": base = 0; break;
            case "D": base = 2; break;
            case "E": base = 4; break;
            case "F": base = 5; break;
            case "G": base = 7; break;
            case "A": base = 9; break;
            case "B": base = 11; break;
            default: base = 0; break;
        }
        return (octave + 1) * 12 + base;
    }
}
