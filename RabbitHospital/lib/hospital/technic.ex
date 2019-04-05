defmodule Hospital.Technic do
  use GenServer
  ## client API

  def start(spec1, spec2, opts \\ []) do
    GenServer.start_link(__MODULE__, [{:spec1, spec1}, {:spec2, spec2} | opts])
  end

  def stop(ref) do
    GenServer.stop(ref)
  end

  ## Server callbacks
  def terminate(_reason, state) do
    AMQP.Basic.cancel(state.channel1, state.tag1)
    AMQP.Basic.cancel(state.channel2, state.tag2)
    AMQP.Channel.close(state.channel1)
    AMQP.Channel.close(state.channel2)
    AMQP.Channel.close(state.channel3)
    AMQP.Connection.close(state.connection)
  end

  def init(opts) do
    {:ok, connection} = AMQP.Connection.open()
    {:ok, channel1} = AMQP.Channel.open(connection)
    {:ok, channel2} = AMQP.Channel.open(connection)
    {:ok, channel3} = AMQP.Channel.open(connection)
    {:ok, _} = AMQP.Queue.declare(channel3, "logs")
    {:ok, _} = AMQP.Queue.declare(channel1, opts[:spec1])
    {:ok, _} = AMQP.Queue.declare(channel2, opts[:spec2])
    {:ok, tag1} = AMQP.Basic.consume(channel1, opts[:spec1])
    {:ok, tag2} = AMQP.Basic.consume(channel2, opts[:spec2])

    {:ok,
     %{
       connection: connection,
       channel1: channel1,
       channel2: channel2,
       channel3: channel3,
       queue1: opts[:spec1],
       queue2: opts[:spec2],
       tag1: tag1,
       tag2: tag2
     }}
  end

  def handle_info({:basic_consume_ok, %{consumer_tag: _consumer_tag}}, state) do
    {:noreply, state}
  end

  def handle_info({:basic_deliver, msg, meta}, state) do
    AMQP.Basic.publish(
      state.channel3,
      "",
      "logs",
      "technican get request: #{msg} done"
    )


    AMQP.Basic.publish(
      state.channel3,
      "",
      meta.reply_to,
      "#{msg} done"
    )

    {:noreply, state}
  end
end
