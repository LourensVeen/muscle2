/*
* Copyright 2008, 2009 Complex Automata Simulation Technique (COAST) consortium
* Copyright 2010-2013 Multiscale Applications on European e-Infrastructures (MAPPER) project
*
* GNU Lesser General Public License
* 
* This file is part of MUSCLE (Multiscale Coupling Library and Environment).
* 
* MUSCLE is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* MUSCLE is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
* 
* You should have received a copy of the GNU Lesser General Public License
* along with MUSCLE.  If not, see <http://www.gnu.org/licenses/>.
*/
 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package muscle.util.concurrency;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import muscle.exception.ExceptionListener;

/**
 * A thread pool that is principle bounded to a fixed number of threads, but will
 * create more threads if it detects there might be a deadlock.
 * The threshold for a deadlock is set to 50 ms of inactivity with a non-empty queue.
 * Threads are destroyed if there is no work for them for over 2 seconds.
 * @author Joris Borgdorff
 */
public class LimitedThreadPool extends SafeThread {
	private final int limit;
	private final static long TIMEOUT_THREAD = 2;
	private final static long TIMEOUT_NEXTGET = 100;
	private int numberOfRunners;
	private int numberOfWaiting;
	private final LinkedBlockingQueue<TaskFuture<?>> queue;
	private final TaskFuture<?> EMPTY = new TaskFuture<Object>(null);
	private long lastGet;
	private final Object counterLock = new Object();
	private final ExceptionListener listener;

	public LimitedThreadPool(int limit, ExceptionListener listener) {
		super("LimitedThreadPool");
		this.limit = limit;
		this.numberOfRunners = 0;
		this.numberOfWaiting = 0;
		this.lastGet = Long.MAX_VALUE;
		queue = new LinkedBlockingQueue<TaskFuture<?>>();
		this.listener = listener;
	}
	
	public <T> Future<T> submit(NamedCallable<T> task) {
		this.tryInactiveRunner();
		TaskFuture<T> fut = new TaskFuture<T>(task);
		queue.add(fut);
		return fut;
	}

	private TaskFuture<?> getNextTask() throws InterruptedException {
		synchronized (counterLock) {
			this.numberOfWaiting++;
		}
		try {
			return queue.poll(TIMEOUT_THREAD, TimeUnit.SECONDS);
		} finally {
			synchronized (counterLock) {
				this.lastGet = System.currentTimeMillis();
				this.numberOfWaiting--;
			}
		}
	}
	
	private void tryInactiveRunner() {
		if (this.isDisposed()) {
			throw new RejectedExecutionException("Executor was halted");
		}
		synchronized (counterLock) {
			if (this.numberOfWaiting == 0 && this.numberOfRunners < this.limit) {
				new TaskRunner().start();
				this.numberOfRunners++;
			}
		}
		if (isDisposed()) {
			this.queue.add(EMPTY);
		}
	}
		
	private void runnerIsDisposed() {
		synchronized (counterLock) {
			this.numberOfRunners--;
		}
	}

	@Override
	public void dispose() {
		synchronized (this) {
			if (isDisposed()) {
				return;
			}
			super.dispose();
		}
		this.queue.clear();
		
		synchronized (counterLock) {
			for (int i = 0; i < this.numberOfRunners; i++) {
				this.queue.add(EMPTY);
			}
		}
	}

	@Override
	protected void handleInterruption(InterruptedException ex) {
		// Do nothing
	}

	@Override
	protected void handleException(Throwable ex) {
		listener.fatalException(ex);
	}

	@Override
	protected synchronized boolean continueComputation() throws InterruptedException {
		if (!isDisposed()) {
			wait(TIMEOUT_NEXTGET+1);
		}
		return !isDisposed();
	}
	
	@Override
	protected void execute() {
		long diffTime = System.currentTimeMillis();
		synchronized (counterLock) {		
			diffTime -= lastGet;
		}
		if (!this.queue.isEmpty() && diffTime > TIMEOUT_NEXTGET) {
			synchronized (counterLock) {
				new TaskRunner().start();
				this.numberOfRunners++;
				if (this.numberOfRunners > this.limit) {
					this.queue.add(EMPTY);
				}
			}
		}
	}
	
	private class TaskRunner extends SafeThread {
		private TaskFuture<?> taskFuture;
		TaskRunner() {
			super("threadpool-taskrunner");
			this.taskFuture = null;
		}
		
		@Override
		protected void handleInterruption(InterruptedException ex) {
			this.handleException(ex);
		}

		@Override
		protected synchronized void handleException(Throwable ex) {
			if (taskFuture != null) {
				taskFuture.setException(ex);
			}
		}

		@Override
		protected boolean continueComputation() throws InterruptedException {
			taskFuture = getNextTask();
			return taskFuture != null && taskFuture.task != null;
		}
		
		@Override
		public synchronized void dispose() {
			runnerIsDisposed();
			super.dispose();
		}
		
		@Override
		protected void execute() throws Exception {
			this.setName("threadpool-taskrunner-" + taskFuture.task.getName());
			taskFuture.calculateResult();
			taskFuture = null;
		}
	}
	
	private class TaskFuture<T> implements Future<T> {
		private final BlockingQueue<T> resultQueue;
		private T result;
		private boolean resultIsSet;
		private ExecutionException except;
		private Thread runningThread;
		private Thread waitingThread;
		private boolean cancelled;
		final NamedCallable<T> task;
		
		TaskFuture(NamedCallable<T> task) {
			this.resultQueue = new ArrayBlockingQueue<T>(1);
			this.result = null;
			this.resultIsSet = false;
			this.except = null;
			this.runningThread = null;
			this.waitingThread = null;
			this.cancelled = false;
			this.task = task;
		}
		
		void calculateResult() throws Exception {
			synchronized (this) {
				this.runningThread = Thread.currentThread();
			}
			T res = this.task.call();
				
			synchronized (this) {
				this.runningThread = null;
				this.result = res;
				this.resultIsSet = true;
				resultQueue.offer(res);
			}
		}
		
		synchronized void setException(Throwable ex) {
			this.runningThread = null;
			if (this.except == null) {
				this.except = new ExecutionException(ex);
			}
			if (waitingThread != null) {
				waitingThread.interrupt();
			}
		}
		
		@Override
		public synchronized boolean cancel(boolean mayInterruptIfRunning) {
			this.cancelled = true;
			if(this.resultIsSet) {
				return false;
			}
			setException(new CancellationException("Task was cancelled"));
			if (mayInterruptIfRunning) {
				runningThread.interrupt();
			}
			return true;
		}

		@Override
		public synchronized boolean isCancelled() {
			return cancelled;
		}

		@Override
		public synchronized boolean isDone() {
			return this.resultIsSet;
		}

		private T getAll(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			synchronized (this) {
				if (this.except != null) {
					ExecutionException thrExcept = this.except;
					this.except = null;
					throw thrExcept;
				} else if (this.resultIsSet) {
					return result;
				}
				this.waitingThread = Thread.currentThread();
			}
			try {
				T res;
				if (unit == null) {
					res = resultQueue.take();
				} else {
					res = resultQueue.poll(timeout, unit);
				}
				synchronized (this) {
					waitingThread = null;
				}
				if (res == null) {
					throw new TimeoutException("Getting value timed out");
				}
			} catch (InterruptedException ex) {
				synchronized (this) {
					this.waitingThread = null;
					if (this.except != null) {
						ExecutionException thrExcept = this.except;
						this.except = null;
						throw thrExcept;
					} else {
						throw ex;
					}
				}
			}
			
			synchronized (this) {
				return result;
			}				
		}
		
		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return this.getAll(timeout, unit);
		}
		@Override
		public T get() throws InterruptedException, ExecutionException {
			try {
				return this.getAll(0L, null);
			} catch (TimeoutException ex) {
				return null; // Should not happen!
			}
		}
	}
}
