package dev.prasadgaikwad.openclaw4j.tool;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that automatically captures the return value of every
 * {@link org.springframework.ai.tool.annotation.Tool}-annotated method
 * on any {@link AITool} bean and registers it in {@link ToolResultStore}.
 *
 * <p>
 * This makes the recovery re-prompt in
 * {@link dev.prasadgaikwad.openclaw4j.agent.AgentPlanner} work correctly
 * for <em>all</em> tools — not just the ones that manually call
 * {@code ToolResultStore.set()}. Individual tool implementations do not
 * need to be modified.
 * </p>
 *
 * <p>
 * The pointcut targets any method annotated with {@code @Tool} declared on a
 * class that implements {@code AITool}, which is the established marker
 * interface for all local tools in this project.
 * </p>
 *
 * @author Prasad Gaikwad
 * @see ToolResultStore
 * @see AITool
 */
@Aspect
@Component
public class ToolResultStoreAspect {

    private static final Logger log = LoggerFactory.getLogger(ToolResultStoreAspect.class);

    /**
     * Intercepts every {@code @Tool}-annotated method on an {@link AITool} bean,
     * executes it normally, then stores the String result in {@link ToolResultStore}
     * so that the agent's recovery re-prompt can include the actual tool output.
     *
     * @param pjp the proceeding join point
     * @return the original return value of the tool method
     * @throws Throwable if the tool method itself throws
     */
    @Around("within(dev.prasadgaikwad.openclaw4j.tool.AITool+) " +
            "&& @annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object captureToolResult(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();

        if (result instanceof String toolOutput && !toolOutput.isBlank()) {
            log.debug("Captured tool result from {}#{}: {} chars",
                    pjp.getTarget().getClass().getSimpleName(),
                    pjp.getSignature().getName(),
                    toolOutput.length());
            ToolResultStore.set(toolOutput);
        }

        return result;
    }
}
