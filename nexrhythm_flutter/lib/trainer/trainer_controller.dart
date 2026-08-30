import 'package:flutter/foundation.dart';

import '../rhythm_engine/rhythm_scheduler.dart';
import '../rhythm_engine/rhythm_timing.dart';

class TrainerController extends ChangeNotifier {
  TrainerController({this.bpm = 120, this.subdivision = 4})
    : assert(bpm > 0),
      assert(subdivision >= 1 && subdivision <= 8);

  final double bpm;
  final int subdivision;

  RhythmScheduler? _scheduler;
  RhythmTick? _currentTick;

  bool get isRunning => _scheduler?.isRunning ?? false;

  RhythmTick? get currentTick => _currentTick;

  void start() {
    if (isRunning) {
      return;
    }

    _scheduler = RhythmScheduler(
      timing: RhythmTiming(bpm: bpm, subdivision: subdivision),
      onTick: _handleTick,
    );

    _scheduler!.start();
  }

  void stop() {
    final scheduler = _scheduler;

    if (scheduler == null) {
      return;
    }

    scheduler.dispose();
    _scheduler = null;
    _currentTick = null;

    notifyListeners();
  }

  void _handleTick(RhythmTick tick) {
    _currentTick = tick;
    notifyListeners();
  }

  @override
  void dispose() {
    _scheduler?.dispose();
    super.dispose();
  }
}
