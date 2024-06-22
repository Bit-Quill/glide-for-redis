/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.configuration;

import glide.api.models.configuration.ClusterSubscriptionConfiguration.PubSubClusterChannelMode;
import glide.api.models.configuration.StandaloneSubscriptionConfiguration.PubSubChannelMode;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Client subscription configuration. Could be either {@link StandaloneSubscriptionConfiguration} or
 * {@link ClusterSubscriptionConfiguration}.
 */
@Getter
@SuperBuilder
public abstract class BaseSubscriptionConfiguration {

    /**
     * A channel subscription mode. Could be either {@link PubSubChannelMode} or {@link
     * PubSubClusterChannelMode}.
     */
    public interface ChannelMode {}

    /**
     * Callback is called for every incoming message. The arguments are:
     *
     * <ol>
     *   <li>A received message
     *   <li>User-defined context
     * </ol>
     */
    // TODO change message type
    public interface MessageCallback extends BiConsumer<Object, Object> {}

    /**
     * Optional callback to accept the incoming messages. See {@link MessageCallback}.<br>
     * If not set, messages will be available via TODO method.<br>
     */
    protected final Optional<MessageCallback> callback;

    /**
     * Optional arbitrary context, which will be passed to callback along with all received messages.
     * <br>
     * Could be used to distinguish clients if multiple clients use a shared callback.
     */
    protected final Optional<Object> context;

    // all code below is a `SuperBuilder` extension to provide user-friendly
    // API `callback` to add a callback [and a context]
    public abstract static class BaseSubscriptionConfigurationBuilder<
            C, B extends BaseSubscriptionConfigurationBuilder<C, B>> {

        protected <M extends ChannelMode> void addSubscription(
                Map<M, Set<String>> subscriptions, M mode, String channelOrPattern) {
            if (!subscriptions.containsKey(mode)) {
                subscriptions.put(mode, new HashSet<>());
            }
            subscriptions.get(mode).add(channelOrPattern);
        }

        /**
         * Set a callback and a context.
         *
         * @param callback The {@link #callback}.
         * @param context The {@link #context}.
         */
        public B callback(MessageCallback callback, Object context) {
            this.callback = Optional.ofNullable(callback);
            this.context = Optional.ofNullable(context);
            return self();
        }

        /**
         * Set a callback without context. <code>null</code> will be supplied to all callback calls as a
         * context.
         *
         * @param callback The {@link #callback}.
         */
        public B callback(MessageCallback callback) {
            this.callback = Optional.ofNullable(callback);
            return self();
        }
    }
}
