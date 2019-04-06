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
  def terminate(_reason, state) do
    AMQP.Basic.cancel(state.channel, state.tag)
    AMQP.Queue.delete(state.channel, state.queue)
    AMQP.Channel.close(state.channel)
    AMQP.Connection.close(state.connection)
  end

  def init(opts) do
    {:ok, connection} = AMQP.Connection.open()
    {:ok, channel} = AMQP.Channel.open(connection)

    {:ok, %{queue: queue_name}} = AMQP.Queue.declare(channel, "", exclusive: true)
    {:ok, tag} = AMQP.Basic.consume(channel, queue_name, nil, no_ack: true)

    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec1])
    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec2])
    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec3])
    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec4])

    :ok = AMQP.Exchange.declare(channel, "with_logs", :direct)

    for {_, q_name} <- opts do
      AMQP.Queue.bind(channel, q_name, "with_logs", routing_key: q_name)
      AMQP.Queue.bind(channel, opts[:spec4], "with_logs", routing_key: q_name)
    end

    AMQP.Exchange.declare(channel, "admin_says", :direct)
    AMQP.Queue.bind(channel,queue_name,"admin_says",routing_key: "admin")

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

  def handle_info({:basic_deliver, message, meta}, state) do
    IO.puts(message)
    AMQP.Basic.ack(state.channel, meta.delivery_tag)
    {:noreply, state}
  end

  def handle_info(msg, state) do
    IO.puts msg
    {:noreply, state}
  end

  def handle_cast({:request, name, type}, state) do
    message = "#{name} #{type}"
    AMQP.Basic.publish(state.channel, "with_logs",type, message,reply_to: state.queue)
    IO.puts "Send request [#{type}] #{message}"
    {:noreply, state}
  end
end
