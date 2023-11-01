package ti4.aspectj;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import ti4.message.BotLogger;

@Aspect
public class ProfilingAspect {

  @Around("@annotation(ti4.aspectj.Timed)")
  public Object profileMethodsWithTimedAnnotation(ProceedingJoinPoint pjp) throws Throwable {
    System.out.println("IN METHOD 1");
    StopWatch stopWatch = new StopWatch(getClass().getSimpleName());
    try {
      stopWatch.start();
      return pjp.proceed();
    } finally {
      stopWatch.stop();
      long timedSeconds = stopWatch.getTime(TimeUnit.SECONDS);
      Method method = ((MethodSignature) pjp.getSignature()).getMethod();
      if (timedSeconds >= method.getAnnotation(Timed.class).thresholdSeconds()) {
        BotLogger.log("Method took " + timedSeconds + " to complete: " + method);
      }
    }
  }

  @Around("execution(* ti4.MessageListener.onSlashCommandInteraction(..)) && args(event)")
  public Object profileMethodsWithTimedCommandParameterAnnotation(ProceedingJoinPoint pjp, SlashCommandInteractionEvent event) throws Throwable {
    System.out.println("IN METHOD 2");
    StopWatch stopWatch = new StopWatch(getClass().getSimpleName());
    try {
      stopWatch.start();
      return pjp.proceed();
    } finally {
      stopWatch.stop();
      long timedSeconds = stopWatch.getTime(TimeUnit.SECONDS);
      Method method = ((MethodSignature) pjp.getSignature()).getMethod();
      if (event == null) {
        BotLogger.log("Improper use of @TimedCommand for method: " + method);
      } else if (timedSeconds >= method.getAnnotation(Timed.class).thresholdSeconds()) {
        BotLogger.log("Method took " + timedSeconds + " to complete: " + method + ", command was: " + event.getFullCommandName());
      }
    }
  }
}
