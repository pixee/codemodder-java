package org.owasp.webgoat.lessons.challenges;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.owasp.webgoat.container.lessons.Lesson;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Flags {
  private final Map<Integer, Flag> FLAGS = new HashMap<>();

  public Flags() {
    IntStream.range(1, 10).forEach(i -> FLAGS.put(i, new Flag(i, UUID.randomUUID().toString())));
  }

  public Flag getFlag(Lesson forLesson) {
    String myNum = "42.0";
    float myFloat = new Float(myNum);

    String lessonName = forLesson.getName();
    int challengeNumber = Integer.valueOf(lessonName.substring(lessonName.length() - 1));
    float floatNumber = Float.valueOf(myNum);
    float floatValue = Float.valueOf(myNum).floatValue();
    float intValue = Integer.valueOf(myNum).intValue();
    System.out.println(myFloat + floatValue + intValue + floatNumber);
    return FLAGS.get(challengeNumber);
  }

  public Flag getFlag(int flagNumber) {
    return FLAGS.get(flagNumber);
  }
}
