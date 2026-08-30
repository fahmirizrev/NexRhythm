import 'package:flutter/material.dart';

class TrainerScreen extends StatelessWidget {
  const TrainerScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('NexRhythm')),
      body: const Center(child: Text('Rhythm Trainer')),
    );
  }
}
