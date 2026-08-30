import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:nexrhythm_flutter/rhythm_engine/rhythm_scheduler.dart';
import 'package:nexrhythm_flutter/rhythm_engine/rhythm_timing.dart';

void main() {
  group('RhythmScheduler', () {
    test('emits the first beat immediately on start', () {
      final clock = _FakeRhythmClock();
      final timers = _FakeTimerFactory();
      final ticks = <RhythmTick>[];

      final scheduler = RhythmScheduler(
        timing: const RhythmTiming(bpm: 120, subdivision: 4),
        onTick: ticks.add,
        clock: clock,
        timerFactory: timers.call,
      );

      scheduler.start();

      expect(scheduler.isRunning, isTrue);
      expect(ticks, hasLength(1));

      expect(ticks.first.step, 0);
      expect(ticks.first.beatIndex, 0);
      expect(ticks.first.subdivisionIndex, 0);
      expect(ticks.first.isBeat, isTrue);
      expect(ticks.first.targetOffset, Duration.zero);

      scheduler.dispose();
    });

    test('schedules subdivisions from absolute target offsets', () {
      final clock = _FakeRhythmClock();
      final timers = _FakeTimerFactory();
      final ticks = <RhythmTick>[];

      final scheduler = RhythmScheduler(
        timing: const RhythmTiming(bpm: 120, subdivision: 4),
        onTick: ticks.add,
        clock: clock,
        timerFactory: timers.call,
      );

      scheduler.start();

      expect(timers.scheduled, hasLength(1));
      expect(timers.scheduled.first.delay, const Duration(milliseconds: 125));

      clock.elapsed = const Duration(milliseconds: 130);
      timers.scheduled.first.timer.fire();

      expect(ticks, hasLength(2));
      expect(ticks.last.step, 1);
      expect(ticks.last.subdivisionIndex, 1);
      expect(ticks.last.targetOffset, const Duration(milliseconds: 125));

      expect(timers.scheduled, hasLength(2));
      expect(timers.scheduled.last.delay, const Duration(milliseconds: 120));

      scheduler.dispose();
    });

    test('marks the first subdivision of each beat as a beat', () {
      final clock = _FakeRhythmClock();
      final timers = _FakeTimerFactory();
      final ticks = <RhythmTick>[];

      final scheduler = RhythmScheduler(
        timing: const RhythmTiming(bpm: 120, subdivision: 4),
        onTick: ticks.add,
        clock: clock,
        timerFactory: timers.call,
      );

      scheduler.start();

      for (var step = 1; step <= 4; step++) {
        clock.elapsed = Duration(milliseconds: 125 * step);
        timers.scheduled[step - 1].timer.fire();
      }

      expect(ticks, hasLength(5));

      final secondBeat = ticks.last;

      expect(secondBeat.step, 4);
      expect(secondBeat.beatIndex, 1);
      expect(secondBeat.subdivisionIndex, 0);
      expect(secondBeat.isBeat, isTrue);
      expect(secondBeat.targetOffset, const Duration(milliseconds: 500));

      scheduler.dispose();
    });

    test('start is idempotent while already running', () {
      final clock = _FakeRhythmClock();
      final timers = _FakeTimerFactory();
      final ticks = <RhythmTick>[];

      final scheduler = RhythmScheduler(
        timing: const RhythmTiming(bpm: 120, subdivision: 4),
        onTick: ticks.add,
        clock: clock,
        timerFactory: timers.call,
      );

      scheduler.start();
      scheduler.start();

      expect(ticks, hasLength(1));
      expect(timers.scheduled, hasLength(1));

      scheduler.dispose();
    });

    test('stop cancels future ticks', () {
      final clock = _FakeRhythmClock();
      final timers = _FakeTimerFactory();
      final ticks = <RhythmTick>[];

      final scheduler = RhythmScheduler(
        timing: const RhythmTiming(bpm: 120, subdivision: 4),
        onTick: ticks.add,
        clock: clock,
        timerFactory: timers.call,
      );

      scheduler.start();

      final pendingTimer = timers.scheduled.first.timer;

      scheduler.stop();

      expect(scheduler.isRunning, isFalse);
      expect(pendingTimer.isActive, isFalse);

      pendingTimer.fire();

      expect(ticks, hasLength(1));

      scheduler.dispose();
    });
  });
}

class _FakeRhythmClock implements RhythmClock {
  @override
  Duration elapsed = Duration.zero;

  bool isRunning = false;

  @override
  void reset() {
    elapsed = Duration.zero;
  }

  @override
  void start() {
    isRunning = true;
  }

  @override
  void stop() {
    isRunning = false;
  }
}

class _FakeTimerFactory {
  final List<_ScheduledFakeTimer> scheduled = [];

  Timer call(Duration delay, void Function() callback) {
    final timer = _FakeTimer(callback);

    scheduled.add(_ScheduledFakeTimer(delay: delay, timer: timer));

    return timer;
  }
}

class _ScheduledFakeTimer {
  const _ScheduledFakeTimer({required this.delay, required this.timer});

  final Duration delay;
  final _FakeTimer timer;
}

class _FakeTimer implements Timer {
  _FakeTimer(this._callback);

  final void Function() _callback;

  bool _isActive = true;
  int _tick = 0;

  void fire() {
    if (!_isActive) {
      return;
    }

    _isActive = false;
    _tick++;
    _callback();
  }

  @override
  void cancel() {
    _isActive = false;
  }

  @override
  bool get isActive => _isActive;

  @override
  int get tick => _tick;
}
