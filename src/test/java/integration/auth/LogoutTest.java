package integration.auth;

import integration.VertxNubesTestBase;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import mock.auth.MockUser;

import org.junit.Test;

public class LogoutTest extends VertxNubesTestBase {

	@Override
	protected JsonArray getControllerPackages() {
		return new JsonArray().add("mock.controllers.auth");
	}

	@Test
	public void testLogout(TestContext context) {
		Async async = context.async();
		getJSON("/private/logout?access_token=" + ApiAuthTest.VALID_TOKEN, response -> {
			context.assertEquals(204, response.statusCode());
			context.assertTrue(MockUser.FLAGGED_AS_CLEARED);
			async.complete();
		});

	}
}
