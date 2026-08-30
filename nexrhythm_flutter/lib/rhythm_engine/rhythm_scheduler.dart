import 'dart:async';

import 'rhythm_timing.dart';

typedef RhythmTickCallback = void Function(RhythmTick tick);
typedef RhythmTimerFactory =
    Timer Function(Duration delay, void Function() callback);

class RhythmTick {
  const RhythmTick({
    required this.step,
    required this.beatIndex,
    required this.subdivisionIndex,
    required this.targetOffset,
  });

  final int step;
  final int beatIndex;
  final int subdivisionIndex;
  final Duration targetOffset;

  bool get isBeat => subdivisionIndex == 0;
}

abstract interface class RhythmClock {
  Duration get elapsed;

  void start();

  void stop();

  void reset();
}

class StopwatchRhythmClock implements RhythmClock {
  final Stopwatch _stopwatch = Stopwatch();

  @override
  Duration get elapsed => _stopwatch.elapsed;

  @override
  void start() {
    _stopwatch.start();
  }

  @override
  void stop() {
    _stopwatch.stop();
  }

  @override
  void reset() {
    _stopwatch.reset();
  }
}

class RhythmScheduler {
  RhythmScheduler({
    required this._timing,
    required this._onTick,
    RhythmClock? clock,
    RhythmTimerFactory? timerFactory,
  }) : _clock = clock ?? StopwatchRhythmClock(),
       _timerFactory = timerFactory ?? _createTimer;

  final RhythmTiming _timing;
  final RhythmTickCallback _onTick;
  final RhythmClock _clock;
  final RhythmTimerFactory _timerFactory;

  Timer? _timer;
  int _nextStep = 0;
  bool _isRunning = false;

  bool get isRunning => _isRunning;

  void start() {
    if (_isRunning) {
      return;
    }

    _isRunning = true;
    _nextStep = 0;

    _clock
      ..reset()
      ..start();

    _emitStep(_nextStep);
    _nextStep++;

    _scheduleNext();
  }

  void stop() {
    if (!_isRunning) {
      return;
    }

    _isRunning = false;

    _timer?.cancel();
    _timer = null;

    _clock
      ..stop()
      ..reset();

    _nextStep = 0;
  }

  void dispose() {
    stop();
  }

  void _scheduleNext() {
    if (!_isRunning) {
      return;
    }

    final targetOffset = _timing.offsetForStep(_nextStep);
    final remaining = targetOffset - _clock.elapsed;

    final delay = remaining.isNegative ? Duration.zero : remaining;

    _timer = _timerFactory(delay, _handleTimer);
  }

  void _handleTimer() {
    if (!_isRunning) {
      return;
    }

    _emitStep(_nextStep);
    _nextStep++;

    _scheduleNext();
  }

  void _emitStep(int step) {
    _onTick(
      RhythmTick(
        step: step,
        beatIndex: step ~/ _timing.subdivision,
        subdivisionIndex: step % _timing.subdivision,
        targetOffset: _timing.offsetForStep(step),
      ),
    );
  }

  static Timer _createTimer(Duration delay, void Function() callback) {
    return Timer(delay, callback);
  }
}
