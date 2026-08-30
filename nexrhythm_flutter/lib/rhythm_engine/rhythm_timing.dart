class RhythmTiming {
  const RhythmTiming({required this.bpm, required this.subdivision})
    : assert(bpm > 0),
      assert(subdivision >= 1 && subdivision <= 8);

  final double bpm;
  final int subdivision;

  static const double microsecondsPerMinute = 60000000;

  double get beatDurationMicroseconds {
    return microsecondsPerMinute / bpm;
  }

  double get subdivisionDurationMicroseconds {
    return beatDurationMicroseconds / subdivision;
  }

  Duration get beatDuration {
    return Duration(microseconds: beatDurationMicroseconds.round());
  }

  Duration get subdivisionDuration {
    return Duration(microseconds: subdivisionDurationMicroseconds.round());
  }

  Duration offsetForStep(int step) {
    if (step < 0) {
      throw RangeError.value(step, 'step', 'Must be zero or greater.');
    }

    return Duration(
      microseconds: (subdivisionDurationMicroseconds * step).round(),
    );
  }
}
