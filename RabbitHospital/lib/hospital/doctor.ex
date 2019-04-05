defmodule Hospital.Doctor do
  use GenServer
  ## client API

  def start(opts \\ []) do
    GenServer.start_link(__MODULE__, [{:spec1, "knee"}, {:spec2, "hip"},{:spec3, "elbow"} | opts])
  end

  def examination(ref, name, type) do
    GenServer.cast(ref, {:request, name, type})
  end

  def stop(ref) do
    GenServer.stop(ref)
  end

  ## Server callbacks
  def terminate(_reason, state) do
    AMQP.Basic.cancel(state.channel, state.tag)
    AMQP.Channel.close(state.channel)
    AMQP.Connection.close(state.connection)
  end

  def init(opts) do
    {:ok, connection} = AMQP.Connection.open()
    {:ok, channel} = AMQP.Channel.open(connection)
    {:ok, %{queue: queue_name}} = AMQP.Queue.declare(channel,"",exclusive: true)
    {:ok, _} = AMQP.Queue.declare(channel, "logs")
    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec1])
    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec2])
    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec3])
    {:ok,tag}=AMQP.Basic.consume(channel, queue_name, nil, no_ack: true)
    {:ok, %{connection: connection, channel: channel, queue: queue_name, tag: tag}}
  end

  def handle_info({:basic_consume_ok,_msg}, state) do
    {:noreply, state}
  end

  def handle_info({:basic_deliver, message, _meta}, state) do
    IO.puts(message)
    {:noreply, state}
  end

  def handle_cast({:request, name, type}, state) do
    AMQP.Basic.publish(
      state.channel,
      "",
      "logs",
      "doctor recieved response: #{name} #{type}"
    )

    AMQP.Basic.publish(
      state.channel,
      "",
      type,
      "#{name} #{type}",
      reply_to: state.queue
      # correlation_id: correlation_id
    )
    {:noreply, state}
  end
end
