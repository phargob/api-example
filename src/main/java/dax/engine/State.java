package dax.engine;

public enum State {
    /*
      Exits out WebWalker and returns true
    */
    EXIT_OUT_WALKER_SUCCESS,


    /*
      Exits out WebWalker and returns false
    */
    EXIT_OUT_WALKER_FAIL,


    /*
      Resumes walker
    */
    CONTINUE_WALKER
}
