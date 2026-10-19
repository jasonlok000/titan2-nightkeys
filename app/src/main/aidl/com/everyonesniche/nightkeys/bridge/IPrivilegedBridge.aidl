package com.everyonesniche.nightkeys.bridge;
import android.os.Bundle;
interface IPrivilegedBridge {
    Bundle inspect() = 1;
    void enableContinuous() = 2;
    void disableLighting() = 3;
    boolean verifyContinuous() = 4;
    void restore(String originalTimeout) = 5;
    void destroy() = 16777114;
}
