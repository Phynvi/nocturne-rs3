package net.nocturne.executor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.nocturne.websocket.Websocket;
import net.nocturne.utils.Logger;
import net.nocturne.utils.Utils;

public class WebsocketThread extends Thread {

	private static final long UPLINK = 1024 * 1024 * 200; // 200 mbps
	private static final long UPLINK_MAX = 1024 * 1024 * 2000; // 2 gbps
	private static final List<Websocket> sessions = Collections
			.synchronizedList(new CopyOnWriteArrayList<Websocket>());

	protected WebsocketThread() {
		setPriority(Thread.MAX_PRIORITY);
		setName("Websocket Thread");
	}

	@Override
	public void run() {
		long limit = UPLINK;
		long last_sleep = Utils.currentTimeMillis();

		// System.out.println("Next step: run method");

		while (!GameExecutorManager.executorShutdown) {
			try {
				long t_start = Utils.currentTimeMillis();
				int processed = 0;
				for (Websocket websocket : sessions) {
					if (websocket.processNext(limit) > 0)
						processed++;
				}

				long now = Utils.currentTimeMillis();
				if (processed < 1 || ((now - last_sleep) > 100)) {
					Thread.sleep(1);
					last_sleep = Utils.currentTimeMillis();
				}

				long t_took = Utils.currentTimeMillis() - t_start;
				if (t_took < 1)
					t_took = 1;

				if (processed < 1)
					limit = UPLINK * t_took;
				else
					limit = (UPLINK * t_took) / processed;

				if (limit > UPLINK_MAX)
					limit = UPLINK_MAX;
			} catch (Throwable t) {
				Logger.handle(t);
			}
		}
	}

	public static void add(Websocket websocket) {
		sessions.add(websocket);
	}

	public static void remove(Websocket websocket) {
		sessions.remove(websocket);
	}
}