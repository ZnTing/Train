package fr.sncf.osrd.train;

import static java.lang.Math.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fr.sncf.osrd.utils.*;
import fr.sncf.osrd.utils.graph.EdgeDirection;
import java.util.*;
import java.util.List;
import java.util.Objects;

public final class TrainPositionTracker implements Cloneable, DeepComparable<TrainPositionTracker> {

    /** Distance covered since the train has departed */
    private double headPathPosition = 0;

    /** Distance covered by the tail since the train has departed */
    private double tailPathPosition = 0;

    /** The path of the train */
    public final List<TrackSectionRange> trackSectionPath;

    /** Keys are positions in the paths, values are the sum of the slopes (per meter) before that point
     * See: https://en.wikipedia.org/wiki/Prefix_sum
     * To get the average train grade between two points, get the difference and divide by the length (m) */
    private final SortedDoubleMap prefixSumTrainGrade;

    /** Cache to avoid recomputing the prefix sum
     * Keys are lists of track section ranges, values are precomputed position trackers at their start positions */
    private static HashMap<List<TrackSectionRange>, TrainPositionTracker> cachedPositionTrackers
            = new HashMap<>();

    /**
     * Create a new position tracker on some given infrastructure and path.
     */
    public TrainPositionTracker(
            double headPathPosition,
            double tailPathPosition,
            List<TrackSectionRange> trackSectionPath,
            SortedDoubleMap prefixSumTrainGrade
    ) {
        this.trackSectionPath = trackSectionPath;
        this.prefixSumTrainGrade = prefixSumTrainGrade;
        this.headPathPosition = headPathPosition;
        this.tailPathPosition = tailPathPosition;
    }

    private TrainPositionTracker(TrainPositionTracker tracker) {
        this.prefixSumTrainGrade = tracker.prefixSumTrainGrade;
        this.headPathPosition = tracker.headPathPosition;
        this.tailPathPosition = tracker.tailPathPosition;
        this.trackSectionPath = tracker.trackSectionPath;
    }

    /** Creates a location from a path (placed at its beginning) */
    public static TrainPositionTracker from(TrainPath path) {
        return from(path.trackSectionPath);
    }

    /** Creates a location from a list of track ranges */
    public static TrainPositionTracker from(List<TrackSectionRange> tracks) {
        var cachedTracker = cachedPositionTrackers.computeIfAbsent(tracks, (key) -> {
            var prefixSumGrade = initPrefixSumGrade(key);
            return new TrainPositionTracker(0, 0,
                    key, prefixSumGrade);
        });
        return cachedTracker.clone();
    }

    // region STD_OVERRIDES

    @Override
    @SuppressFBWarnings({"FE_FLOATING_POINT_EQUALITY"})
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (obj.getClass() != TrainPositionTracker.class)
            return false;

        var other = (TrainPositionTracker) obj;
        return trackSectionPath.equals(other.trackSectionPath)
                && headPathPosition == other.headPathPosition
                && tailPathPosition == other.tailPathPosition;
    }

    @Override
    public int hashCode() {
        return Objects.hash(trackSectionPath, headPathPosition, tailPathPosition);
    }
    // endregion

    /** Resets the position tracker, but keeps the pre-computed data */
    public TrainPositionTracker reset() {
        headPathPosition = 0;
        tailPathPosition = 0;
        return this;
    }

    private static SortedDoubleMap initPrefixSumGrade(List<TrackSectionRange> trackSectionPath) {
        var res = new SortedDoubleMap();
        var sumPreviousRangeLengths = 0.;
        var sumSlope = 0.;
        for (var range : trackSectionPath) {
            var grades = range.edge.forwardGradients
                    .getValuesInRange(range.getBeginPosition(), range.getEndPosition());
            NavigableSet<Range> keys = new TreeSet<>(grades.keySet());
            if (range.direction == EdgeDirection.STOP_TO_START) {
                grades = range.edge.backwardGradients
                    .getValuesInRange(range.getBeginPosition(), range.getEndPosition());
                keys = new TreeSet<>(grades.keySet()).descendingSet();
            }

            for (var interval : keys) {
                var begin = sumPreviousRangeLengths + abs(range.getBeginPosition() - interval.getBeginPosition());
                var end = sumPreviousRangeLengths + abs(range.getBeginPosition() - interval.getEndPosition());
                var first = min(begin, end);
                var last = max(begin, end);
                res.put(first, sumSlope);
                sumSlope += grades.get(interval) * interval.length();
                res.put(last, sumSlope);
            }

            sumPreviousRangeLengths += range.length();
        }
        return res;
    }

    /**
     * Makes a copy of the position tracker.
     * @return a copy of the position tracker.
     */
    @Override
    public TrainPositionTracker clone() {
        return new TrainPositionTracker(this);
    }

    public TrackSectionLocation getHeadLocation() {
        return TrainPath.findLocation(headPathPosition, trackSectionPath);
    }

    /**
     * Updates the position of the train on the network.
     * @param positionDelta How much the train moves by.
     */
    public void updatePosition(double expectedTrainLength, double positionDelta) {
        headPathPosition += positionDelta;
        tailPathPosition = headPathPosition - expectedTrainLength;
    }


    public double getPathPosition() {
        return headPathPosition;
    }

    /** Computes the average grade (slope) under the train. */
    public double meanTrainGrade() {
        var sum = prefixSumTrainGrade.interpolate(headPathPosition)
                - prefixSumTrainGrade.interpolate(tailPathPosition);
        return sum / (headPathPosition - tailPathPosition);
    }

    @Override
    @SuppressFBWarnings({"FE_FLOATING_POINT_EQUALITY"})
    public boolean deepEquals(TrainPositionTracker other) {
        return tailPathPosition == other.tailPathPosition
                && headPathPosition == other.headPathPosition
                && DeepEqualsUtils.deepEquals(trackSectionPath, other.trackSectionPath);
    }

    @Override
    public String toString() {
        return String.format("TrainPositionTracker { head=%f, tail=%f }", headPathPosition, tailPathPosition);
    }
}
