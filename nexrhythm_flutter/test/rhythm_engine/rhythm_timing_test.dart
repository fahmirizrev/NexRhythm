import 'package:flutter_test/flutter_test.dart';
import 'package:nexrhythm_flutter/rhythm_engine/rhythm_timing.dart';

void main() {
  group('RhythmTiming', () {
    test('calculates beat duration from BPM', () {
      const timing = RhythmTiming(bpm: 120, subdivision: 1);

      expect(timing.beatDurationMicroseconds, 500000);
      expect(timing.beatDuration, const Duration(milliseconds: 500));
    });

    test('calculates even subdivision duration', () {
      const timing = RhythmTiming(bpm: 120, subdivision: 4);

      expect(timing.subdivisionDurationMicroseconds, 125000);
      expect(timing.subdivisionDuration, const Duration(milliseconds: 125));
    });

    test('supports triplet subdivision without cumulative rounding drift', () {
      const timing = RhythmTiming(bpm: 120, subdivision: 3);

      expect(
        timing.subdivisionDurationMicroseconds,
        closeTo(166666.66666666666, 0.000001),
      );

      expect(timing.offsetForStep(1), const Duration(microseconds: 166667));
      expect(timing.offsetForStep(2), const Duration(microseconds: 333333));
      expect(timing.offsetForStep(3), const Duration(microseconds: 500000));
    });

    test('supports subdivisions from 1 through 8', () {
      for (var subdivision = 1; subdivision <= 8; subdivision++) {
        final timing = RhythmTiming(bpm: 120, subdivision: subdivision);

        expect(
          timing.offsetForStep(subdivision),
          const Duration(milliseconds: 500),
        );
      }
    });

    test('rejects negative step offsets', () {
      const timing = RhythmTiming(bpm: 120, subdivision: 4);

      expect(() => timing.offsetForStep(-1), throwsRangeError);
    });
  });
}
