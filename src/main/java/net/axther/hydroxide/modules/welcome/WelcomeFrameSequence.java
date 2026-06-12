package net.axther.hydroxide.modules.welcome;

import java.util.List;

public final class WelcomeFrameSequence {

    private final List<Frame> frames;
    private final int frameIntervalTicks;
    private final boolean loop;

    public WelcomeFrameSequence(List<Frame> frames, int frameIntervalTicks, boolean loop) {
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("At least one welcome frame is required.");
        }
        this.frames = List.copyOf(frames);
        this.frameIntervalTicks = Math.max(1, frameIntervalTicks);
        this.loop = loop;
    }

    public Frame frameAtTick(long tick) {
        int index = (int) Math.max(0, tick / frameIntervalTicks);
        if (loop) {
            return frames.get(index % frames.size());
        }
        return frames.get(Math.min(index, frames.size() - 1));
    }

    public int frameIntervalTicks() {
        return frameIntervalTicks;
    }

    public int frameCount() {
        return frames.size();
    }

    public long durationTicks() {
        return (long) frameIntervalTicks * frames.size();
    }

    public List<Frame> frames() {
        return frames;
    }

    public record Frame(String title, String subtitle) {
    }
}
