package io;

/**
 * @author : Siyadong Xiong (sx225@cornell.edu)
 * @version : 3/26/17
 */
public class JoinGameMsg{
    private boolean success;

    public JoinGameMsg(boolean success) {
        this.success = success;
    }

    public boolean getSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
