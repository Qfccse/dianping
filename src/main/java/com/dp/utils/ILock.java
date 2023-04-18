package com.dp.utils;

public interface ILock {
    public boolean tryLock(Long timeSec);
    public void unLock();
}
