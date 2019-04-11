defmodule Hospital.Doctor do
  use GenServer
  ## client API

  def start() do
    GenServer.start_link(__MODULE__, [
      {:spec1, "knee"},
      {:spec2, "hip"},
      {:spec3, "elbow"},
      {:spec4, "logs"}
    ])
  end

  def examine(ref, name, type) do
    GenServer.cast(ref, {:request, name, type})
  end

  def stop(ref) do
    GenServer.stop(ref)
  end

  ## Server callbacks

  def init(opts) do
    {:ok, connection} = AMQP.Connection.open()
    {:ok, channel} = AMQP.Channel.open(connection)
    {:ok, %{queue: queue_name}} = AMQP.Queue.declare(channel, "", exclusive: true)
    {:ok, tag} = AMQP.Basic.consume(channel, queue_name)

    Enum.map(opts, fn {_, q_name} -> AMQP.Queue.declare(channel, q_name) end)

    :ok = AMQP.Exchange.declare(channel, "with_logs", :topic)

    AMQP.Exchange.declare(channel, "admin_says", :fanout)
    AMQP.Queue.bind(channel, queue_name, "admin_says")

    for {_, q_name} <- [{:self_queue_name, "#{queue_name}"} | opts] do
      AMQP.Queue.bind(channel, q_name, "with_logs", routing_key: "#{q_name}.#")
    end

    AMQP.Queue.bind(channel, opts[:spec4], "with_logs", routing_key: "#.log")

    {:ok,
     %{
       connection: connection,
       channel: channel,
       queue: queue_name,
       tag: tag,
       log_to: opts[:spec4]
     }}
  end

  def handle_info({:basic_consume_ok, _msg}, state) do
    {:noreply, state}
  end

  def handle_info({:basic_deliver_ok, _message, _meta}, state) do
    {:noreply, state}
  end

  def handle_info({:basic_deliver, msg, meta}, state) do
    message = Hospital.Message.from_message(msg)

    case message.message_type do
      :ann ->
        IO.puts("*** DOCTOR RECIEVED MESSAGE \n**** #{message.body}")

      :response ->
        IO.puts(
          "*** DOCTOR RECIEVED RESPONSE \n**** #{message.examination} #{message.status} #{
            message.who
          }"
        )
    end

    AMQP.Basic.ack(state.channel, meta.delivery_tag)
    {:noreply, state}
  end

  def handle_cast({:request, name, type}, state) do
    message = %Hospital.Message{
      name: name,
      message_type: :request,
      examination: type,
      status: "not done",
      who: name
    }

    AMQP.Basic.publish(
      state.channel,
      "with_logs",
      "#{type}.log",
      Hospital.Message.to_message(message),
      reply_to: state.queue
    )

    IO.puts("Send request [#{type}]")
    {:noreply, state}
  end

  def terminate(_reason, state) do
    Enum.map(state.tag, fn x -> Hospital.Utils.cancel(state.channel, x) end)
    AMQP.Queue.delete(state.channel, state.queue)
    AMQP.Channel.close(state.channel)
    AMQP.Connection.close(state.connection)
  end
end
