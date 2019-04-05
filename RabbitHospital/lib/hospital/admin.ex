defmodule Hospital.Admin do
  use GenServer

  ## Client API

  def start(opts \\ []) do
    GenServer.start(__MODULE__, [{:spec1, "knee"}, {:spec2, "hip"},{:spec3, "elbow"} | opts])
  end

  def stop(ref) do
    GenServer.stop(ref)
  end

  ## Server callbacks
  def init(opts) do
    {:ok, connection} = AMQP.Connection.open()
    {:ok, channel} = AMQP.Channel.open(connection)
    {:ok, _} = AMQP.Queue.declare(channel, "logs")
    {:ok, tag} = AMQP.Basic.consume(channel, "logs")
    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec1])
    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec2])
    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec3])
    {:ok, %{connection: connection, channel: channel, tag: tag, opts: opts}}
  end

  def handle_info({:basic_consume_ok,_msg}, state) do
    {:noreply, state}
  end

  def handle_info({:basic_deliver, message, _meta}, state) do
    IO.puts(message)
    {:noreply, state}
  end

  def terminate(_reason, state) do
    {:ok, _} = AMQP.Queue.delete(state.channel, state.opts[:spec1])
    {:ok, _} = AMQP.Queue.delete(state.channel, state.opts[:spec2])
    {:ok, _} = AMQP.Queue.delete(state.channel, state.opts[:spec3])
    {:ok, _} = AMQP.Queue.delete(state.channel, "logs")
    AMQP.Basic.cancel(state.channel, state.tag)
    AMQP.Channel.close(state.channel)
    AMQP.Connection.close(state.connection)
  end
end
