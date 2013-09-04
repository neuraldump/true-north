package tn.common.engine;

/**
 * Represents a unit (let be data or unit of work) that is distribute-able. In
 * the case of orchestrating a sort process of distributed sorting; it is the
 * data that when distributed in return distributes the processing across
 * multiple processing units. At any given time a distribute-able unit can be in
 * any of the following state<br>
 * <br>
 * 
 * [READY_4_DISTR,DISTR_IN_PROGRESS,READY,DISTR_ERROR]
 * 
 * @author Senthu Sivasambu, September 2013
 * 
 */
public interface Distributable<F, T> {

	public static enum DistrState {

		READY_4_DISTR("Ready For Distribution", 0), DISTR_IN_PROGRESS(
				"Distribution In Progress", 2), DISTR_ERROR("Finished", 4), ERRORED(
				"Error occured", 8);

		public String msg;
		public String statusCode;
		public String auxMsg;

		DistrState(String msg, int statusCode) {
			this.msg = msg;
			this.auxMsg = "";
		}

		@Override
		public String toString() {
			return "[TNS" + statusCode + "] " + msg + ":" + auxMsg;
		}
	}

	public DistrState distrStatus();

	/**
	 * makes a copy of @param from and then distribute it to @param to
	 * 
	 * @param from
	 *            - from location
	 * @param to
	 *            - copy to where
	 */
	public void distribute(F from, T to);

}
