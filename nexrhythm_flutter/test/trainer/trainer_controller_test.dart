import 'package:flutter_test/flutter_test.dart';
import 'package:nexrhythm_flutter/trainer/trainer_controller.dart';

void main() {
  group('TrainerController', () {
    test('uses the default Trainer configuration', () {
      final controller = TrainerController();
      addTearDown(controller.dispose);

      expect(controller.bpm, 120);
      expect(controller.subdivision, 4);
      expect(controller.isRunning, isFalse);
      expect(controller.currentTick, isNull);
    });

    test('starts on the first beat and subdivision', () {
      final controller = TrainerController();
      addTearDown(controller.dispose);

      controller.start();

      expect(controller.isRunning, isTrue);
      expect(controller.currentTick, isNotNull);
      expect(controller.currentTick!.step, 0);
      expect(controller.currentTick!.beatIndex, 0);
      expect(controller.currentTick!.subdivisionIndex, 0);
      expect(controller.currentTick!.isBeat, isTrue);
    });

    test('stop clears the current runtime tick', () {
      final controller = TrainerController();
      addTearDown(controller.dispose);

      controller.start();
      controller.stop();

      expect(controller.isRunning, isFalse);
      expect(controller.currentTick, isNull);
    });
  });
}
