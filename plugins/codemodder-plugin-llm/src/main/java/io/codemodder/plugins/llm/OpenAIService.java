package io.codemodder.plugins.llm;

import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.HttpException;

/**
 * A custom service class to call the {@link OpenAiApi}, since the out-of-the box {@link
 * OpenAiService} <a href="https://github.com/TheoKanning/openai-java/issues/189">does not support
 * automatic retries</a>.
 */
public class OpenAIService {
  private final OpenAiApi api;

  public OpenAIService(final String token) {
    this.api = OpenAiService.buildApi(token, Duration.ofSeconds(90));
  }

  public ChatCompletionResult createChatCompletion(final ChatCompletionRequest request) {
    return this.api
        .createChatCompletion(request)
        .retryWhen(new OpenAIRetryStrategy())
        .blockingGet();
  }
}

/**
 * When there is a retryable error from OpenAI -- either a timeout or a retryable <a
 * href="https://platform.openai.com/docs/guides/error-codes/api-errors">error code</a> -- this will
 * retry the request up to 3 times, with a delay of {@code 1 * retryCount} seconds between retries.
 */
class OpenAIRetryStrategy implements Function<Flowable<? extends Throwable>, Flowable<Object>> {
  private static final int MAX_RETRY_COUNT = 3;
  private static final Logger logger = LoggerFactory.getLogger(OpenAIRetryStrategy.class);

  private int retryCount = 0;

  @Override
  public Flowable<Object> apply(final Flowable<? extends Throwable> flowable) {
    return flowable.flatMap(
        e -> {
          if (++retryCount <= MAX_RETRY_COUNT && isRetryable(e)) {
            logger.warn("retrying after {}s: {}", retryCount, e);
            return Flowable.timer(retryCount, TimeUnit.SECONDS);
          } else {
            return Flowable.error(e);
          }
        });
  }

  private boolean isRetryable(final Throwable e) {
    if (e instanceof SocketTimeoutException) {
      return true;
    } else if (e instanceof HttpException) {
      int code = ((HttpException) e).code();
      return code == 429 || code == 500 || code == 503;
    }
    return false;
  }
}
