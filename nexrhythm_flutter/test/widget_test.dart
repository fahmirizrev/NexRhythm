import 'package:flutter_test/flutter_test.dart';
import 'package:nexrhythm_flutter/app/nexrhythm_app.dart';

void main() {
  testWidgets('NexRhythm opens the Trainer screen', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const NexRhythmApp());

    expect(find.text('NexRhythm'), findsOneWidget);
    expect(find.text('Rhythm Trainer'), findsOneWidget);
  });
}
