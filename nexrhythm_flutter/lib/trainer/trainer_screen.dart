import 'package:flutter/material.dart';

import 'trainer_controller.dart';

class TrainerScreen extends StatefulWidget {
  const TrainerScreen({super.key});

  @override
  State<TrainerScreen> createState() => _TrainerScreenState();
}

class _TrainerScreenState extends State<TrainerScreen> {
  late final TrainerController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TrainerController();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('NexRhythm')),
      body: Center(
        child: ListenableBuilder(
          listenable: _controller,
          builder: (context, child) {
            final tick = _controller.currentTick;

            return Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text('Rhythm Trainer'),
                const SizedBox(height: 24),
                Text('BPM: ${_controller.bpm.toStringAsFixed(0)}'),
                Text('Subdivision: ${_controller.subdivision}'),
                const SizedBox(height: 24),
                Text(tick == null ? 'Beat: --' : 'Beat: ${tick.beatIndex + 1}'),
                Text(
                  tick == null
                      ? 'Subdivision Step: --'
                      : 'Subdivision Step: ${tick.subdivisionIndex + 1}',
                ),
                const SizedBox(height: 24),
                FilledButton(
                  onPressed: _controller.isRunning
                      ? _controller.stop
                      : _controller.start,
                  child: Text(_controller.isRunning ? 'Stop' : 'Start'),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}
