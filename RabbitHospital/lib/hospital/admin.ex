defmodule Hospital.Admin do
  use GenServer

  ## Client API

  def start() do
    GenServer.start(__MODULE__, [
      {:spec1, "knee"},
      {:spec2, "hip"},
      {:spec3, "elbow"},
      {:spec4, "logs"}
    ])
  end

  def announce(ref, msg) do
    GenServer.cast(ref, {:global, msg})
  end

  def stop(ref) do
    GenServer.stop(ref)
  end

  ## Server callbacks
  def init(opts) do
    {:ok, connection} = AMQP.Connection.open()
    {:ok, channel} = AMQP.Channel.open(connection)
    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec1])
    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec2])
    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec3])
    {:ok, _} = AMQP.Queue.declare(channel, opts[:spec4])
    {:ok, tag} = AMQP.Basic.consume(channel, opts[:spec4])
    

    AMQP.Exchange.declare(channel, "with_logs", :direct)
    AMQP.Exchange.declare(channel, "admin_says", :direct)

    for {_, q_name} <- opts do
      AMQP.Queue.bind(channel, q_name, "with_logs", routing_key: q_name)
      AMQP.Queue.bind(channel, opts[:spec4], "with_logs", routing_key: q_name)
    end

    {:ok, %{connection: connection, channel: channel, tag: tag, opts: opts}}
  end

  def handle_info({:basic_consume_ok, _msg}, state) do
    {:noreply, state}
  end

  def handle_info({:basic_deliver, message, meta}, state) do
    IO.puts(message)
    AMQP.Basic.ack(state.channel, meta.delivery_tag)
    {:noreply, state}
  end

  def handle_info(_msg, state) do
    IO.puts "random msg"
    {:noreply, state}
  end

  def handle_cast({:global, msg}, state) do
    AMQP.Basic.publish(state.channel, "admin_says","admin", msg)
    {:noreply, state}
  end

  def terminate(_reason, state) do
    {:ok, _} = AMQP.Queue.delete(state.channel, state.opts[:spec1])
    {:ok, _} = AMQP.Queue.delete(state.channel, state.opts[:spec2])
    {:ok, _} = AMQP.Queue.delete(state.channel, state.opts[:spec3])
    {:ok, _} = AMQP.Queue.delete(state.channel, state.opts[:spec4])
    AMQP.Basic.cancel(state.channel, state.tag)
    AMQP.Channel.close(state.channel)
    AMQP.Connection.close(state.connection)
  end
end
