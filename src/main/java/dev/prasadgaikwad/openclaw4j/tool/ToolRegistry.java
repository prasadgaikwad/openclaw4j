package dev.prasadgaikwad.openclaw4j.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry to manage and discover tools.
 */
@Service
public class ToolRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);
    private final List<Object> tools = new ArrayList<>();

    public ToolRegistry(List<AITool> tools) {
        this.tools.addAll(tools);
        logger.info("Found {} tool beans.", tools.size());
    }

    /**
     * Returns all discovered tool objects.
     */
    public List<Object> getTools() {
        return tools;
    }
}
