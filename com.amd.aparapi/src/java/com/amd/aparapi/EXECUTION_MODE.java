package com.amd.aparapi;

import java.util.LinkedHashSet;
import java.util.logging.Logger;

import com.amd.aparapi.internal.opencl.OpenCLLoader;

/**
 * The <i>execution mode</i> ENUM enumerates the possible modes of executing a kernel.
 * One can request a mode of execution using the values below, and query a kernel after it first executes to
 * determine how it executed.
 *
 * <p>
 * Aparapi supports 4 execution modes.
 * <ul>
 * <table>
 * <tr><th align="left">Enum value</th><th align="left">Execution</th></tr>
 * <tr><td><code><b>GPU</b></code></td><td>Execute using OpenCL on first available GPU device</td></tr>
 * <tr><td><code><b>CPU</b></code></td><td>Execute using OpenCL on first available CPU device</td></tr>
 * <tr><td><code><b>JTP</b></code></td><td>Execute using a Java Thread Pool (one thread spawned per available core)</td></tr>
 * <tr><td><code><b>SEQ</b></code></td><td>Execute using a single loop. This is useful for debugging but will be less
 * performant than the other modes</td></tr>
 * </table>
 * </ul>
 * <p>
 * To request that a kernel is executed in a specific mode, call <code>Kernel.setExecutionMode(EXECUTION_MODE)</code> before the
 *  kernel first executes.
 * <p>
 * <blockquote><pre>
 *     int[] values = new int[1024];
 *     // fill values array
 *     SquareKernel kernel = new SquareKernel(values);
 *     kernel.setExecutionMode(Kernel.EXECUTION_MODE.JTP);
 *     kernel.execute(values.length);
 * </pre></blockquote>
 * <p>
 * Alternatively, the property <code>com.amd.aparapi.executionMode</code> can be set to one of <code>JTP,GPU,CPU,SEQ</code>
 * when an application is launched.
 * <p><blockquote><pre>
 *    java -classpath ....;aparapi.jar -Dcom.amd.aparapi.executionMode=GPU MyApplication
 * </pre></blockquote><p>
 * Generally setting the execution mode is not recommended (it is best to let Aparapi decide automatically) but the option
 * provides a way to compare a kernel's performance under multiple execution modes.
 *
 * @author  gfrost AMD Javalabs
 * @version Alpha, 21/09/2010
 */

public enum EXECUTION_MODE {
   /**
    * A dummy value to indicate an unknown state.
    */
   NONE,
   /**
    * The value representing execution on a GPU device via OpenCL.
    */
   GPU,
   /**
    * The value representing execution on a CPU device via OpenCL.
    * <p>
    * <b>Note</b> not all OpenCL implementations support OpenCL compute on the CPU.
    */
   CPU,
   /**
    * The value representing execution on a Java Thread Pool.
    * <p>
    * By default one Java thread is started for each available core and each core will execute <code>globalSize/cores</code> work items.
    * This creates a total of <code>globalSize%cores</code> threads to complete the work.
    * Choose suitable values for <code>globalSize</code> to minimize the number of threads that are spawned.
    */
   JTP,
   /**
    * The value representing execution sequentially in a single loop.
    * <p>
    * This is meant to be used for debugging a kernel.
    */
   SEQ;

   private static Logger logger = Logger.getLogger(Config.getLoggerName());

   public static EXECUTION_MODE getDefaultExecutionMode() {
      EXECUTION_MODE defaultExecutionMode = OpenCLLoader.isOpenCLAvailable() ? GPU : JTP;
      final String executionMode = Config.executionMode;
      if (executionMode != null) {
         try {
            EXECUTION_MODE requestedExecutionMode;
            requestedExecutionMode = getExecutionModeFromString(executionMode).iterator().next();
            logger.fine("requested execution mode =");
            if ((OpenCLLoader.isOpenCLAvailable() && requestedExecutionMode.isOpenCL()) || !requestedExecutionMode.isOpenCL()) {
               defaultExecutionMode = requestedExecutionMode;
            }
         } catch (final Throwable t) {
            // we will take the default
         }
      }

      logger.fine("default execution modes = " + defaultExecutionMode);

      return (defaultExecutionMode);
   }

   public static LinkedHashSet<EXECUTION_MODE> getDefaultExecutionModes() {
      LinkedHashSet<EXECUTION_MODE> defaultExecutionModes = new LinkedHashSet<EXECUTION_MODE>();

      if (OpenCLLoader.isOpenCLAvailable()) {
         defaultExecutionModes.add(GPU);
         defaultExecutionModes.add(JTP);
      } else {
         defaultExecutionModes.add(JTP);
      }

      final String executionMode = Config.executionMode;

      if (executionMode != null) {
         try {
            LinkedHashSet<EXECUTION_MODE> requestedExecutionModes;
            requestedExecutionModes = EXECUTION_MODE.getExecutionModeFromString(executionMode);
            logger.fine("requested execution mode =");
            for (final EXECUTION_MODE mode : requestedExecutionModes) {
               logger.fine(" " + mode);
            }
            if ((OpenCLLoader.isOpenCLAvailable() && EXECUTION_MODE.anyOpenCL(requestedExecutionModes))
                  || !EXECUTION_MODE.anyOpenCL(requestedExecutionModes)) {
               defaultExecutionModes = requestedExecutionModes;
            }
         } catch (final Throwable t) {
            // we will take the default
         }
      }

      logger.info("default execution modes = " + defaultExecutionModes);

      for (final EXECUTION_MODE e : defaultExecutionModes) {
         logger.info("SETTING DEFAULT MODE: " + e.toString());
      }

      return (defaultExecutionModes);
   }

   static LinkedHashSet<EXECUTION_MODE> getExecutionModeFromString(String executionMode) {
      final LinkedHashSet<EXECUTION_MODE> executionModes = new LinkedHashSet<EXECUTION_MODE>();
      for (final String mode : executionMode.split(",")) {
         executionModes.add(valueOf(mode.toUpperCase()));
      }
      return executionModes;
   }

   public static EXECUTION_MODE getFallbackExecutionMode() {
      final EXECUTION_MODE defaultFallbackExecutionMode = JTP;
      logger.info("fallback execution mode = " + defaultFallbackExecutionMode);
      return (defaultFallbackExecutionMode);
   }

   static boolean anyOpenCL(LinkedHashSet<EXECUTION_MODE> _executionModes) {
      for (final EXECUTION_MODE mode : _executionModes) {
         if ((mode == GPU) || (mode == CPU)) {
            return true;
         }
      }
      return false;
   }

   public boolean isOpenCL() {
      return (this == GPU) || (this == CPU);
   }
}
