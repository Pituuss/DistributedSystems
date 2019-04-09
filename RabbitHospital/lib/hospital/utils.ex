defmodule Hospital.Utils do
  def cancel(channel, consumer_tag) do
    {:ok, ^consumer_tag} = AMQP.Basic.cancel(channel, consumer_tag)

    receive do
      {:basic_cancel_ok, %{consumer_tag: ^consumer_tag}} ->
        {:ok, consumer_tag}
    end
  end
end
