defmodule Hospital.Technic do
  use GenServer

  # @allowed = ["knee","hip","elbow"]
  ## client API

  def start(spec1, spec2) do
    GenServer.start_link(__MODULE__, [{:spec1, spec1}, {:spec2, spec2}, {:spec4, "logs"}])
  end

  def stop(ref) do
    GenServer.stop(ref)
  end

  ## Server callbacks
  def init(opts) do
    {:ok, connection} = AMQP.Connection.open()
    {:ok, channel} = AMQP.Channel.open(connection)

    Enum.map(opts, fn {_, q_name} -> AMQP.Queue.declare(channel, q_name) end)

    {:ok, tag1} = AMQP.Basic.consume(channel, opts[:spec1])
    {:ok, tag2} = AMQP.Basic.consume(channel, opts[:spec2])

    AMQP.Exchange.declare(channel, "with_logs", :topic)
    AMQP.Queue.bind(channel, opts[:spec4], "with_logs", routing_key: "#.log")

    for {_, q_name} <- opts do
      AMQP.Queue.bind(channel, q_name, "with_logs", routing_key: "#{q_name}.#")
    end

    AMQP.Exchange.declare(channel, "admin_says", :fanout)
    {:ok, %{queue: queue_name}} = AMQP.Queue.declare(channel, "", exclusive: true)

    AMQP.Queue.bind(channel, queue_name, "admin_says")

    {:ok, %{queue: queue_name}} = AMQP.Queue.declare(channel, "", exclusive: true)
    {:ok, tag} = AMQP.Basic.consume(channel, queue_name, nil, no_ack: true)

    {:ok,
     %{
       connection: connection,
       channel: channel,
       log_to: opts[:spec4],
       tag: [tag, tag1, tag2]
     }}
  end

  def handle_info({:basic_consume_ok, _msg}, state) do
    {:noreply, state}
  end

  def handle_info({:basic_deliver_ok, _message, _meta}, state) do
    {:noreply, state}
  end

  def handle_info({:basic_deliver, msg, meta}, state) do
    IO.puts("*** TECHNIC RECIEVED REQUEST")
    r_key = "#{meta.reply_to}.log"
    AMQP.Basic.publish(state.channel, "with_logs", r_key, "#{msg} done")
    AMQP.Basic.ack(state.channel, meta.delivery_tag)
    {:noreply, state}
  end

  def terminate(_reason, state) do
    Enum.map(state.tag, fn x -> Hospital.Utils.cancel(state.channel, x) end)
    AMQP.Channel.close(state.channel)
    AMQP.Connection.close(state.connection)
  end
end
