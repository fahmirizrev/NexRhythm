import 'package:flutter/material.dart';

import '../trainer/trainer_screen.dart';

class NexRhythmApp extends StatelessWidget {
  const NexRhythmApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'NexRhythm',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
      ),
      home: const TrainerScreen(),
    );
  }
}
